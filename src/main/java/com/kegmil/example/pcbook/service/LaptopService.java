package com.kegmil.example.pcbook.service;

import com.kegmil.example.pcbook.elasticSearch.JavaElasticSearch;
import com.kegmil.example.pcbook.SearchFilterHelper.SearchBuildHelper;
import com.kegmil.example.pcbook.elasticSearch.SearchResult;
import com.kegmil.example.pcbook.mapper.JsonHelper;
import com.kegmil.example.pcbook.mapper.ProtoEntityMapper;
import com.kegmil.example.pcbook.pb.*;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LaptopService extends LaptopServiceGrpc.LaptopServiceImplBase {

    private static final Logger logger = Logger.getLogger(LaptopService.class.getName());

    private final LaptopStore laptopStore;
    private JavaElasticSearch javaElasticSearch;

    public LaptopService(LaptopStore laptopStore) {
        this.laptopStore = laptopStore;
    }

    @Override
    public void createLaptop(CreateLaptopRequest request, StreamObserver<CreateLaptopResponse> responseObserver) {
        Laptop laptop = request.getLaptop();
        String id = laptop.getId();
        logger.info("Get a create-laptop request with ID: " + id);

        UUID uuid;
        if (id.isEmpty()) {
            uuid = UUID.randomUUID();
        } else {
            try {
                uuid = UUID.fromString(id);
            } catch (IllegalArgumentException e) {
                responseObserver.onError(
                        Status.INVALID_ARGUMENT
                                .withDescription(e.getMessage())
                                .asRuntimeException());
                return;
            }
        }

        if (Context.current().isCancelled()) {
            logger.info("Request is cancelled");
            responseObserver.onError(Status.CANCELLED.withDescription("Request is cancelled").asRuntimeException());
        }

        Laptop other = laptop.toBuilder().setId(uuid.toString()).build();
        try {
            laptopStore.save(other);
            syncDataToElasticSearch(other);
        } catch (AlreadyExistException e) {
            responseObserver.onError(
                    Status.ALREADY_EXISTS
                            .withDescription(e.getMessage())
                            .asRuntimeException());
            return;
        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
            return;
        }

        CreateLaptopResponse response = CreateLaptopResponse.newBuilder().setId(other.getId()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        logger.info("Saved laptop with ID: " + other.getId());
    }

    public void syncDataToElasticSearch(Laptop laptop) {
        //do something here ? option :)
        String itemPushToElasticSearch = JsonHelper.mapLaptopProtoToJson(laptop);
        laptop.toBuilder().setStorages(0, Storage.newBuilder().setMemory(Memory.newBuilder().setUnit(Memory.Unit.BIT).build()).build());
        javaElasticSearch.pushData("laptopelasticsearch", itemPushToElasticSearch);
    }

    @Override
    public void searchLaptop(SearchLaptopRequest request, StreamObserver<SearchLaptopResponse> responseObserver) {
        Filter filter = request.getFilter();
        logger.info("Got a search-laptop request with filter:\n" + filter);

        laptopStore.search(filter, laptop -> {
            logger.info("Found laptop with ID: " + laptop.getId());
            SearchLaptopResponse response = SearchLaptopResponse.newBuilder().setLaptop(laptop).build();
            responseObserver.onNext(response);
        });

        responseObserver.onCompleted();
        logger.info("Search laptop completed");
    }

    @Override
    public void advancedSearch(SearchLaptopRequest request, StreamObserver<SearchLaptopResponse> responseObserver) {
        Filter filter = request.getFilter();
        logger.info("advancedSearch Laptop:\n" + filter);
//
        //handle code here
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(SearchBuildHelper.buildQueryBuilder(filter));
        //searching on elastic search
        SearchResult<com.kegmil.example.pcbook.data.Laptop> searchResult =
                javaElasticSearch.search("laptopelasticsearch", searchSourceBuilder, com.kegmil.example.pcbook.data.Laptop.class);
        searchResult.getHits().forEach(laptop -> {
//            System.out.println(laptop);
            try {
                Laptop laptopProto = ProtoEntityMapper.toProto(laptop, Laptop.newBuilder()).build();
                SearchLaptopResponse response = SearchLaptopResponse.newBuilder().setLaptop(laptopProto).build();
                responseObserver.onNext(response);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });

        responseObserver.onCompleted();
        logger.info("Advanced Search laptop completed");
    }
}
