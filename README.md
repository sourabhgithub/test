package com.discover.website.apply.transformer;

import com.discover.website.apply.model.ApplicationFormData;
import com.discover.website.apply.model.CombinedAddress;
import com.discover.website.apply.model.Address;
import com.discover.website.apply.model.Indicators;
import com.discover.website.apply.model.LobResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Qualifier("combinedAddressTransformer")
@AllArgsConstructor
@Slf4j
public class ApplicationFormToCombinedAddressTransformer
        implements Transformer<ApplicationFormData, CombinedAddress> {

    private final ObjectMapper objectMapper;

    @Override
    @SneakyThrows
    public CombinedAddress transform(final ApplicationFormData applicationFormData) {
        log.info("lobAttestationEnabled flag is set to: {}. Transforming address to be sent to PET.",
                applicationFormData.isLobAttestationEnabled());

        // Get the LobResponse data
        boolean isMailingSameAsHome = applicationFormData.getIsHomeAddressMailingAddress() == YesOrNo.Yes;
        LobResponse primaryLobResponse = mapLobResponse(applicationFormData, false);
        LobResponse mailingLobResponse = isMailingSameAsHome ? null : mapLobResponse(applicationFormData, true);

        // Build CombinedAddress object
        CombinedAddress.CombinedAddressBuilder builder = CombinedAddress.builder()
                .lobResponse(Optional.ofNullable(primaryLobResponse)) // Home Address LobResponse
                .physical(buildAddress(applicationFormData, primaryLobResponse, false)) // Physical address
                .indicators(buildIndicators(applicationFormData, false)) // Home Address Indicators
                .attributes(buildAttributes(applicationFormData, primaryLobResponse, false)); // Home Address Attributes

        if (!isMailingSameAsHome) {
            builder
                    .mailingAddress(buildAddress(applicationFormData, mailingLobResponse, true)) // Mailing address
                    .indicators(buildIndicators(applicationFormData, true)) // Mailing Address Indicators
                    .attributes(buildAttributes(applicationFormData, mailingLobResponse, true)); // Mailing Address Attributes
        }

        return builder.build();
    }

    private Address buildAddress(ApplicationFormData applicationFormData, LobResponse lobResponse, boolean isMailingAddress) {
        return Address.builder()
                .city(isMailingAddress ? applicationFormData.getMailingCity() : applicationFormData.getCity())
                .addressLine1(isMailingAddress ? applicationFormData.getMailingAddressLine1() : applicationFormData.getAddressLine1())
                .addressLine2(isMailingAddress ? applicationFormData.getMailingUnitNumber() : applicationFormData.getUnitNumber())
                .unitNumber(isMailingAddress ? applicationFormData.getMailingUnitNumber() : applicationFormData.getUnitNumber())
                .zipCode(isMailingAddress ? applicationFormData.getMailingZipCode() : applicationFormData.getZipCode())
                .state(isMailingAddress ? applicationFormData.getMailingState() : applicationFormData.getState())
                .indicators(buildIndicators(applicationFormData, isMailingAddress)) // Indicators
                .attributes(buildAttributes(applicationFormData, lobResponse, isMailingAddress)) // Attributes
                .lobResponse(Optional.ofNullable(lobResponse)) // LobResponse (if applicable)
                .build();
    }

    private Indicators buildIndicators(ApplicationFormData applicationFormData, boolean isMailingAddress) {
        return Indicators.builder()
                .lobAddrValidInd(Optional.ofNullable(isMailingAddress ? applicationFormData.getMailingLobAddrValidInd() : applicationFormData.getLobAddrValidInd()))
                .addrAttestationInd(Optional.ofNullable(isMailingAddress ? applicationFormData.getMailingAddrAttestationInd() : applicationFormData.getAddrAttestationInd()))
                .build();
    }

    private Attributes buildAttributes(ApplicationFormData applicationFormData, LobResponse lobResponse, boolean isMailingAddress) {
        return Attributes.builder()
                .deliverability(Optional.ofNullable(isMailingAddress ? applicationFormData.getMailingDeliverability() : applicationFormData.getDeliverability()))
                .recordType(lobResponse == null ? Optional.empty() : Optional.ofNullable(lobResponse.getComponents().getRecordType()))
                .dpvFootnotes(lobResponse == null ? Optional.empty() : Optional.ofNullable(lobResponse.getDeliverabilityAnalysis().getDpvFootnotes()))
                .pmbDesignator(lobResponse == null ? Optional.empty() : Optional.ofNullable(lobResponse.getComponents().getPmbDesignator()))
                .pmbNumber(lobResponse == null ? Optional.empty() : Optional.ofNullable(lobResponse.getComponents().getPmbNumber()))
                .build();
    }

    @SneakyThrows
    private LobResponse mapLobResponse(final ApplicationFormData applicationFormData, final boolean shouldMapMailingAddress) {
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        String jsonResponse = shouldMapMailingAddress ? applicationFormData.getMailingLobResponse() : applicationFormData.getLobResponse();
        if (jsonResponse == null || jsonResponse.isEmpty()) {
            return null;
        }
        return objectMapper.readValue(jsonResponse, LobResponse.class);
    }
}
