package com.harish.mapper;

import com.harish.dto.PlanDto;
import com.harish.dto.subscription.SubscriptionResponse;
import com.harish.entity.Plan;
import com.harish.entity.Subscription;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

    SubscriptionResponse toSubscriptionResponse(Subscription subscription);

    PlanDto toPlanResponse(Plan plan);

}
