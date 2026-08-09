package io.pragmatic.ponderus.repository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import io.pragmatic.ponderus.domain.User;

public interface UserRepository extends CrudRepository<User, UUID>{
    public Optional<User> findByFirebaseUid(String firebaseUid);
}
