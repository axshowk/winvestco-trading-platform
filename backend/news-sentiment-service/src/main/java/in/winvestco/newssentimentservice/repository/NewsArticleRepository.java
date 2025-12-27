package in.winvestco.newssentimentservice.repository;

import in.winvestco.newssentimentservice.entity.NewsArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {
    Optional<NewsArticle> findByLink(String link);
    boolean existsByLink(String link);
}
