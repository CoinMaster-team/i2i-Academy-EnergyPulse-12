package com.coinmaster.energypulse.home.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class HomeTopologyInitializer implements ApplicationRunner {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(HomeTopologyInitializer.class);

    private final HomeService homeService;

    public HomeTopologyInitializer(HomeService homeService) {
        this.homeService = homeService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int publishedHomeCount = homeService.publishAllHomeTopologies();
        LOGGER.info(
                "Published {} persisted home topology event(s) at startup.",
                publishedHomeCount);
    }
}
