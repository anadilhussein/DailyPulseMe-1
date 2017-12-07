package backend.repository;

import backend.entity.User;
//import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends MongoRepository<User,Long> {
    User findByEmail(@Param("email") String email);
    void deleteByEmail(@Param("email") String email);
}