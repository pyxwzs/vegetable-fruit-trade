package com.trade.dto;

import lombok.Data;

@Data
public class FilePreviewDTO {

    private String fileId;

    private String fileName;

    private String fileType;

    private String previewUrl;

    private String thumbnailUrl;

    private Boolean canPreview;

    private Boolean canDownload;

    private String previewType; // image, pdf, video, audio, office
}
