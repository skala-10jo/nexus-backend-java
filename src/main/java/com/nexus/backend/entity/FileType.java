package com.nexus.backend.entity;

/**
 * File type enumeration for the new file structure.
 * Provides explicit type identification replacing MIME type checking.
 *
 */
public enum FileType {
    /**
     * Document files (PDF, DOCX, TXT, etc.)
     */
    DOCUMENT,

    /**
     * Video files (MP4, AVI, MOV, MKV, etc.)
     */
    VIDEO,

    /**
     * Audio files (MP3, WAV, etc.) - reserved for future use
     */
    AUDIO
}
