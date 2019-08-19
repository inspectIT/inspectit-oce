/**
 * Various constants used by the application.
 */

/**
 * Default <title> content
 */
export const BASE_PAGE_TITLE = "inspectIT Ocelot Configuration Server";

/**
 * Interval in which will be checked, whether the token will be expiring.
 */
export const RENEW_TOKEN_TIME_INTERVAL = 60000;

/**
 * Minimum expiration time
 * If the expiration time of the current token is shorter than the MIN_TOKEN_EXPIRATION_IIME, the token will be renewed.
 */
export const MIN_TOKEN_EXPIRATION_IIME = 300000;

