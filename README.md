package com.discover.website.apply.transformer;

import com.discover.website.apply.model.ApplicationFormData; import com.discover.website.apply.model.CombinedAddress; import com.discover.website.apply.model.Address; import com.discover.website.apply.model.Indicators; import com.discover.website.apply.model.LobResponse; import com.fasterxml.jackson.databind.DeserializationFeature; import com.fasterxml.jackson.databind.ObjectMapper; import lombok.AllArgsConstructor; import lombok.SneakyThrows; import lombok.extern.slf4j.Slf4j; import org.springframework.beans.factory.annotation.Qualifier; import org.springframework.stereotype.Component;

import java.util.Optional;

@Component @Qualifier("combinedAddressTransformer") @AllArgsConstructor @Slf4j public class ApplicationFormToCombinedAddressTransformer implements Transformer<ApplicationFormData, CombinedAddress> {

private final ObjectMapper objectMapper;

@Override
@SneakyThrows
public CombinedAddress transform(final ApplicationFormData data) {
    log.info("lobAttestationEnabled flag is set to: {}. Transforming address to be sent to PET.",
             data.isLobAttestationEnabled());

    boolean sameMailing = data.getIsHomeAddressMailingAddress() == YesOrNo.Yes;
    LobResponse primaryLob = mapLobResponse(data, false);
    LobResponse mailingLob = sameMailing ? null : mapLobResponse(data, true);

    CombinedAddress.CombinedAddressBuilder builder = CombinedAddress.builder()
        .lobResponse(Optional.ofNullable(primaryLob))
        .physical(buildPhysicalAddress(data, primaryLob))
        .attributes(buildIndicators(data, primaryLob))
        .deliverability(Optional.ofNullable(data.getDeliverability()))
        .recordType(extract(() -> primaryLob.getComponents().getRecordType()))
        .dpvFootnotes(extract(() -> primaryLob.getDeliverabilityAnalysis().getDpvFootnotes()))
        .pmbDesignator(extract(() -> primaryLob.getComponents().getPmbDesignator()))
        .pmbNumber(extract(() -> primaryLob.getComponents().getPmbNumber()));

    if (!sameMailing) {
        builder
            .mailingLobResponse(Optional.ofNullable(mailingLob))
            .mailingAddress(buildMailingAddress(data, mailingLob));
    }

    return builder.build();
}

private Address buildPhysicalAddress(ApplicationFormData data, LobResponse lob) {
    return Address.builder()
            .city(data.getCity())
            .addressLine1(data.getAddressLine1())
            .addressLine2(data.getUnitNumber())
            .unitNumber(data.getUnitNumber())
            .zipCode(data.getZipCode())
            .state(data.getState())
            .indicators(buildIndicators(data, lob))
            .build();
}

private Address buildMailingAddress(ApplicationFormData data, LobResponse lob) {
    return Address.builder()
            .city(data.getMailingCity())
            .addressLine1(data.getMailingAddressLine1())
            .addressLine2(data.getMailingUnitNumber())
            .unitNumber(data.getMailingUnitNumber())
            .zipCode(data.getMailingZipCode())
            .state(data.getMailingState())
            .indicators(buildMailingIndicators(data, lob))
            .build();
}

private Indicators buildIndicators(ApplicationFormData data, LobResponse lob) {
    return Indicators.builder()
            .lobAddrValidInd(Optional.ofNullable(data.getLobAddrValidInd()))
            .addrAttestationInd(Optional.ofNullable(data.getAddrAttestationInd()))
            .build();
}

private Indicators buildMailingIndicators(ApplicationFormData data, LobResponse lob) {
    return Indicators.builder()
            .lobAddrValidInd(Optional.ofNullable(data.getMailingLobAddrValidInd()))
            .addrAttestationInd(Optional.ofNullable(data.getMailingAddrAttestationInd()))
            .build();
}

private <T> Optional<T> extract(Supplier<T> supplier) {
    try {
        return Optional.ofNullable(supplier.get());
    } catch (NullPointerException e) {
        return Optional.empty();
    }
}

@SneakyThrows
private LobResponse mapLobResponse(ApplicationFormData data, boolean mailing) {
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    String json = mailing ? data.getMailingLobResponse() : data.getLobResponse();
    if (json == null || json.isEmpty()) {
        return null;
    }
    return objectMapper.readValue(json, LobResponse.class);
}

}

