package oogasalad.data.api;

/**
 * Declarative target selector for a cheat effect.
 *
 * <p>Runtime target resolution owns the meaning of each {@code kind}. Expected initial kinds are
 * {@code mainCharacter}, {@code vehicle}, {@code allDamageable}, {@code componentType}, and
 * {@code world}. The optional {@code id} can narrow selectors such as {@code componentType}.
 *
 * @param kind selector kind interpreted by the cheat target resolver
 * @param id   optional selector value, such as a component type id
 * @author Emilio Garcia
 */
public record CheatTargetInfo(String kind, String id) {

}
