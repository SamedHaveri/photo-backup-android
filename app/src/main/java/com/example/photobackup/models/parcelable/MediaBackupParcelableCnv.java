package com.example.photobackup.models.parcelable;

import com.example.photobackup.data.entity.MediaBackup;

import java.util.ArrayList;
import java.util.List;

public class MediaBackupParcelableCnv {
    public static List<MediaBackup> convert(ArrayList<MediaBackupParcelable> parcels){
        List<MediaBackup> result = new ArrayList<>();
        for(MediaBackupParcelable parcel : parcels){
            MediaBackup i =
                    new MediaBackup(parcel.getUriId(), parcel.getUriPath(), parcel.getAbsolutePath(),
                    parcel.getMediaType(), parcel.getDateAdded(), parcel.getOrientation(),
                    parcel.getUploaded(), parcel.getUploadTries(), parcel.getId());
            result.add(i);
        }
        return result;
    }
}
