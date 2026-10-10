package ee.tuleva.mortgage.document;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface DocumentRepository extends JpaRepository<Document, UUID> {

  List<Document> findByBorrowerIdAndTypeOrderByCreatedAtDesc(UUID borrowerId, DocumentType type);
}
