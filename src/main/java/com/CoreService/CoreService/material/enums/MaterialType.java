package com.CoreService.CoreService.material.enums;

/**
 * Nature of a study material. Everything but {@code LINK} is backed by an
 * uploaded file.
 */
public enum MaterialType {
    PDF,
    DOCUMENT,
    IMAGE,
    VIDEO,
    LINK
}
