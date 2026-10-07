package fr.insee.pogues.client.interceptor;

public record OidcToken(String tokenValue, long tokenExpirationTime) {

    public boolean isExpired(long marginMillis) {
        return System.currentTimeMillis() >= tokenExpirationTime - marginMillis;
    }
}