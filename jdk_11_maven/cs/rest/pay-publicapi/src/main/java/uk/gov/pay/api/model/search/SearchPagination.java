package uk.gov.pay.api.model.search;


import uk.gov.pay.api.model.links.SearchNavigationLinks;

public interface SearchPagination {
    int getCount();

    int getTotal();

    int getPage();

    SearchNavigationLinks getLinks();
}
