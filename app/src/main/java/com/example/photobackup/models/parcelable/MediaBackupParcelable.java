package com.example.photobackup.models.parcelable;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.photobackup.data.entity.MediaBackup;

public class MediaBackupParcelable implements Parcelable {
    private Long uriId;
    private String uriPath;
    private String absolutePath;
    private String mediaType;
    private Long dateAdded;
    private Integer orientation;
    private Boolean uploaded;
    private Byte uploadTries;
    private Long id;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.uriId);
        dest.writeString(this.uriPath);
        dest.writeString(this.absolutePath);
        dest.writeString(this.mediaType);
        dest.writeValue(this.dateAdded);
        dest.writeValue(this.orientation);
        dest.writeValue(this.uploaded);
        dest.writeValue(this.uploadTries);
        dest.writeValue(this.id);
    }

    public void readFromParcel(Parcel source) {
        this.uriId = (Long) source.readValue(Long.class.getClassLoader());
        this.uriPath = source.readString();
        this.absolutePath = source.readString();
        this.mediaType = source.readString();
        this.dateAdded = (Long) source.readValue(Long.class.getClassLoader());
        this.orientation = (Integer) source.readValue(Integer.class.getClassLoader());
        this.uploaded = (Boolean) source.readValue(Boolean.class.getClassLoader());
        this.uploadTries = (Byte) source.readValue(Byte.class.getClassLoader());
        this.id = (Long) source.readValue(Long.class.getClassLoader());
    }

    public MediaBackupParcelable(MediaBackup mediaBackup) {
        this.id = mediaBackup.getId();
        this.absolutePath = mediaBackup.getAbsolutePath();
        this.mediaType = mediaBackup.getMediaType();
        this.dateAdded = mediaBackup.getDateAdded();
        this.orientation = mediaBackup.getOrientation();
        this.uploaded = mediaBackup.getUploaded();
        this.uploadTries = mediaBackup.getUploadTries();
        this.uriId = mediaBackup.getUriId();
        this.uriPath = mediaBackup.getUriPath();
    }

    protected MediaBackupParcelable(Parcel in) {
        this.uriId = (Long) in.readValue(Long.class.getClassLoader());
        this.uriPath = in.readString();
        this.absolutePath = in.readString();
        this.mediaType = in.readString();
        this.dateAdded = (Long) in.readValue(Long.class.getClassLoader());
        this.orientation = (Integer) in.readValue(Integer.class.getClassLoader());
        this.uploaded = (Boolean) in.readValue(Boolean.class.getClassLoader());
        this.uploadTries = (Byte) in.readValue(Byte.class.getClassLoader());
        this.id = (Long) in.readValue(Long.class.getClassLoader());
    }

    public static final Parcelable.Creator<MediaBackupParcelable> CREATOR = new Parcelable.Creator<MediaBackupParcelable>() {
        @Override
        public MediaBackupParcelable createFromParcel(Parcel source) {
            return new MediaBackupParcelable(source);
        }

        @Override
        public MediaBackupParcelable[] newArray(int size) {
            return new MediaBackupParcelable[size];
        }
    };

    public Long getUriId() {
        return uriId;
    }

    public void setUriId(Long uriId) {
        this.uriId = uriId;
    }

    public String getUriPath() {
        return uriPath;
    }

    public void setUriPath(String uriPath) {
        this.uriPath = uriPath;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public Long getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(Long dateAdded) {
        this.dateAdded = dateAdded;
    }

    public Integer getOrientation() {
        return orientation;
    }

    public void setOrientation(Integer orientation) {
        this.orientation = orientation;
    }

    public Boolean getUploaded() {
        return uploaded;
    }

    public void setUploaded(Boolean uploaded) {
        this.uploaded = uploaded;
    }

    public Byte getUploadTries() {
        return uploadTries;
    }

    public void setUploadTries(Byte uploadTries) {
        this.uploadTries = uploadTries;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
