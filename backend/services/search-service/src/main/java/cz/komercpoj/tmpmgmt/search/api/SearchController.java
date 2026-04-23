package cz.komercpoj.tmpmgmt.search.api;

import cz.komercpoj.tmpmgmt.search.api.dto.SearchHit;
import cz.komercpoj.tmpmgmt.search.application.SearchService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService service;

    public SearchController(SearchService service) {
        this.service = service;
    }

    @GetMapping
    public List<SearchHit> search(
            @RequestParam String q,
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        return service.search(q, type, limit);
    }
}
