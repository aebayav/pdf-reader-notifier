package com.pdfFileReader.domain.entity;

/** Asenkron evrak isleme isinin durumu. */
public enum JobStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED
}
