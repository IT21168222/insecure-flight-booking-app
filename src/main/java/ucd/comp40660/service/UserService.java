package ucd.comp40660.service;

import ucd.comp40660.user.model.ConfirmationToken;
import ucd.comp40660.user.model.User;

import java.util.Optional;

public interface UserService {

    void save(User user);

    Optional<User> findByUsername(String username);

    User findByEmail(String email);

    void save(ConfirmationToken confirmationToken);

    User isValidToken(String confirmationToken);

    void savePassword(String email, String password);

}
