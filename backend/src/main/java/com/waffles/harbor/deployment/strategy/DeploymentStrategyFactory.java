package com.waffles.harbor.deployment.strategy;

import com.waffles.harbor.deployment.enums.DeploymentType;
import com.waffles.harbor.exception.DeploymentStateException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DeploymentStrategyFactory {

    private final Map<DeploymentType, DeploymentStrategy> strategies;

    public DeploymentStrategyFactory(List<DeploymentStrategy> strategies){
        this.strategies = strategies.stream()
                .collect(Collectors.toMap(
                        DeploymentStrategy::getType,
                        Function.identity()
                ));
    }

    public DeploymentStrategy getStrategy(DeploymentType type){
        DeploymentStrategy strategy = strategies.get(type);

        if(strategy == null){
            throw new DeploymentStateException(
                    "No strategy found for this deployment type: "+ type
            );
        }

        return strategy;
    }
}
