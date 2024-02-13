package uk.gov.pay.api.model.search;

import black.door.hate.HalRepresentation;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.exception.SearchPaymentsException;
import uk.gov.pay.api.model.links.Link;
import uk.gov.pay.api.model.links.SearchNavigationLinks;

import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

public class PaginationDecorator {

    private final String baseUrl;
    private final List<String> queryParametersToBeExcluded
            = List.of("account_id", "gateway_account_id", "transaction_type", "status_version");

    @Inject
    public PaginationDecorator(PublicApiConfig config) {
        baseUrl = config.getBaseUrl();
    }

    public HalRepresentation.HalRepresentationBuilder decoratePagination(HalRepresentation.HalRepresentationBuilder halRepresentationBuilder,
                                                                         SearchPagination pagination, String path) {

        HalRepresentation.HalRepresentationBuilder builder = addPaginationProperties(halRepresentationBuilder, pagination);
        SearchNavigationLinks links = pagination.getLinks();
        try {
            addLink(builder, "self", transformIntoPublicUri(baseUrl, links.getSelf(), path));
            addLink(builder, "first_page", transformIntoPublicUri(baseUrl, links.getFirstPage(), path));
            addLink(builder, "last_page", transformIntoPublicUri(baseUrl, links.getLastPage(), path));
            addLink(builder, "prev_page", transformIntoPublicUri(baseUrl, links.getPrevPage(), path));
            addLink(builder, "next_page", transformIntoPublicUri(baseUrl, links.getNextPage(), path));
        } catch (URISyntaxException ex) {
            throw new SearchPaymentsException(ex);
        }
        return builder;
    }

    public SearchNavigationLinks transformLinksToPublicApiUri(
            SearchNavigationLinks links, String path) {
        try {
            return links.withSelfLink(transformIntoPublicUriAsString(baseUrl, links.getSelf(), path))
                    .withFirstLink(transformIntoPublicUriAsString(baseUrl, links.getFirstPage(), path))
                    .withLastLink(transformIntoPublicUriAsString(baseUrl, links.getLastPage(), path))
                    .withPrevLink(transformIntoPublicUriAsString(baseUrl, links.getPrevPage(), path))
                    .withNextLink(transformIntoPublicUriAsString(baseUrl, links.getNextPage(), path));
        } catch (URISyntaxException ex) {
            throw new SearchPaymentsException(ex);
        }
    }

    private HalRepresentation.HalRepresentationBuilder addPaginationProperties(HalRepresentation.HalRepresentationBuilder halRepresentationBuilder,
                                                                               SearchPagination pagination) {
        halRepresentationBuilder
                .addProperty("count", pagination.getCount())
                .addProperty("total", pagination.getTotal())
                .addProperty("page", pagination.getPage());
        return halRepresentationBuilder;
    }

    private void addLink(HalRepresentation.HalRepresentationBuilder halRepresentationBuilder, String name, URI uri) {
        if (uri != null) {
            halRepresentationBuilder.addLink(name, uri);
        }
    }

    private String transformIntoPublicUriAsString(String baseUrl, Link link, String path) throws URISyntaxException {
        UriBuilder uriBuilder = getUriBuilder(baseUrl, link, path);

        return Optional.ofNullable(uriBuilder)
                .map(builder -> {
                    // breaks the order of query parameters 
                    queryParametersToBeExcluded.forEach(queryParam ->
                            uriBuilder.replaceQueryParam(queryParam, (Object[]) null));
                    return builder.build().toString();
                })
                .orElse(null);
    }

    private URI transformIntoPublicUri(String baseUrl, Link link, String path) throws URISyntaxException {
        UriBuilder uriBuilder = getUriBuilder(baseUrl, link, path);
        return Optional.ofNullable(uriBuilder)
                .map(builder -> {
                    // breaks the order of query parameters
                    queryParametersToBeExcluded.forEach(queryParam ->
                            uriBuilder.replaceQueryParam(queryParam, (Object[]) null));
                    return builder.build();
                })
                .orElse(null);
    }

    private UriBuilder getUriBuilder(String baseUrl, Link link, String path) throws URISyntaxException {
        if (link == null)
            return null;

        return UriBuilder.fromUri(baseUrl)
                .path(path)
                .replaceQuery(new URI(link.getHref()).getQuery());
    }
}
