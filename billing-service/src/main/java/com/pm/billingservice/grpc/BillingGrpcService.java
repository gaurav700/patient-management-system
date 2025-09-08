package com.pm.billingservice.grpc;
import billing.BillingResponse;
import billing.BillingServiceGrpc.BillingServiceImplBase;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@Slf4j
public class BillingGrpcService extends BillingServiceImplBase {
    @Override
    public void createBillingAccount(billing.BillingRequest request, io.grpc.stub.StreamObserver<billing.BillingResponse> responseObserver) {
        log.info("createBillingAccount request received: {}", request.toString());

        // Billing Service Implementation
        BillingResponse response = BillingResponse.newBuilder()
                .setAccountId("12345")
                .setStatus("ACTIVE")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
