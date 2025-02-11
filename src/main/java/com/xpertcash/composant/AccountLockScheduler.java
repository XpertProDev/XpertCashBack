package com.xpertcash.composant;

import org.springframework.stereotype.Component;

@Component
public class AccountLockScheduler {

    /*@Autowired
    private UsersRepository usersRepository;

    @Autowired
    private UsersService usersService;

    // Exécute la tâche toutes les minutes
    @Scheduled(fixedRate = 60000)
    public void lockInactiveAccounts() {
        List<Users> users = usersRepository.findAll();
        for (Users user : users) {
            usersService.checkAndLockIfInactive(user);
        }
    }*/
}
