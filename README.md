private boolean isValueNotMasked(final String value) {
    final Boolean validationResult = value.toLowerCase().matches(
        "[a-z0-9!#$%&'*+/=?^_`{|}~-]+" + 
        "@[a-z0-9-]*[a-z0-9]" + 
        "(?:\\.[a-z0-9-]*[a-z0-9])?" + 
        "\\.[a-z0-9]{2,}"
    );
    if (Boolean.TRUE.equals(validationResult)) {
        log.info("Masked Email value {}", value);
    }
    return !validationResult;
}
