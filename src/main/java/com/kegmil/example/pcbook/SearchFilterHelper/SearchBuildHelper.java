package com.kegmil.example.pcbook.SearchFilterHelper;

import com.kegmil.example.pcbook.pb.Filter;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
//Anh Nhan Do This
public class SearchBuildHelper {
    public static QueryBuilder buildQueryBuilder(Filter filter) {
        BoolQueryBuilder context = QueryBuilders.boolQuery();

        //a query write here
        context.must(QueryBuilders.matchQuery("priceUsd", filter.getMaxPriceUsd()))
                .must(QueryBuilders.matchQuery("cpu.numberCores", filter.getMinCpuCores()))
                .must(QueryBuilders.matchQuery("cpu.minGhz", filter.getMinCpuGhz()))
                .must(QueryBuilders.matchQuery("ram.value", filter.getMinRam().getValue()));

        return context;
    }
}
