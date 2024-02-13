package uk.gov.pay.api.model.search.dispute;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.api.model.links.SearchNavigationLinks;
import uk.gov.pay.api.model.search.SearchPagination;

import java.util.List;
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DisputesSearchResults implements SearchPagination {
    @Schema(name = "total", example = "100", description = "Number of total disputes matching your search criteria.")
    private int total;
    @Schema(name = "count", example = "20", description = "Number of disputes on the current page of search results.")
    private int count;
    @Schema(name = "page", example = "1", description = "The page of results you’re viewing. To view other pages, make this request again using the 'page' parameter.")
    private int page;
    @Schema(description = "Contains disputes matching your search criteria.")
    private List<DisputeForSearchResult> results;
    @Schema(name = "links", description = "Contains links you can use to move between the pages of this search.")
    SearchNavigationLinks links;

    public DisputesSearchResults(int total, int count, int page, List<DisputeForSearchResult> results,
                                 SearchNavigationLinks links) {
        this.total = total;
        this.count = count;
        this.page = page;
        this.results = results;
        this.links = links;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public int getTotal() {
        return total;
    }

    @Override
    public int getPage() {
        return page;
    }

    @Override
    public SearchNavigationLinks getLinks() {
        return links;
    }

    public List<DisputeForSearchResult> getResults() {
        return results;
    }
}
