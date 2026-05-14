import React, { useState } from 'react';
import { message } from 'antd';
import {
  useGetInterviewPhotosQuery,
  useUploadInterviewPhotoMutation,
  useDeleteInterviewPhotoMutation,
} from '../interviewApi';

const MAX_PHOTOS = 10;
const ALLOWED_TYPES = ['image/jpeg', 'image/png'];
const MAX_SIZE_MB = 5;

interface PhotoGalleryProps {
  interviewId: string;
  readonly?: boolean;
}

const PhotoGallery: React.FC<PhotoGalleryProps> = ({
  interviewId,
  readonly = false,
}) => {
  const [caption, setCaption] = useState('');
  const { data: photos = [] } = useGetInterviewPhotosQuery(interviewId);
  const [uploadPhoto, { isLoading: uploading }] = useUploadInterviewPhotoMutation();
  const [deletePhoto, { isLoading: deleting }] = useDeleteInterviewPhotoMutation();

  async function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!ALLOWED_TYPES.includes(file.type)) {
      message.error('Only JPG and PNG files are allowed.');
      e.target.value = '';
      return;
    }
    if (file.size > MAX_SIZE_MB * 1024 * 1024) {
      message.error(`File size must be under ${MAX_SIZE_MB}MB.`);
      e.target.value = '';
      return;
    }
    if (photos.length >= MAX_PHOTOS) {
      message.error(`Maximum ${MAX_PHOTOS} photos per interview.`);
      e.target.value = '';
      return;
    }
    try {
      await uploadPhoto({
        interviewId,
        file,
        caption: caption || undefined,
      }).unwrap();
      setCaption('');
      message.success('Photo uploaded.');
    } catch {
      message.error('Failed to upload photo. Please try again.');
    }
    e.target.value = '';
  }

  async function handleDelete(photoId: string) {
    try {
      await deletePhoto(photoId).unwrap();
      message.success('Photo removed.');
    } catch {
      message.error('Failed to delete photo.');
    }
  }

  return (
    <div className="pg-root">
      <div className="pg-header">
        <h3 className="pg-title">Interview Photos</h3>
        <span className="pg-count">
          {photos.length} / {MAX_PHOTOS}
        </span>
      </div>
      {!readonly && photos.length < MAX_PHOTOS && (
        <div className="pg-upload-area">
          <div className="si-field">
            <label className="si-label" htmlFor="pg-caption">
              Caption <span className="si-optional">(optional)</span>
            </label>
            <input
              id="pg-caption"
              type="text"
              className="si-input"
              placeholder="Describe the photo…"
              value={caption}
              onChange={e => setCaption(e.target.value)}
            />
          </div>
          <label className="pg-file-btn" htmlFor="pg-file-input">
            {uploading ? 'Uploading…' : '+ Add Photo'}
            <input
              id="pg-file-input"
              type="file"
              accept="image/jpeg,image/png"
              className="pg-file-input-hidden"
              onChange={handleFileChange}
              disabled={uploading}
            />
          </label>
          <p className="si-hint">
            JPG or PNG, max 5 MB each. Up to {MAX_PHOTOS} photos.
          </p>
        </div>
      )}
      {photos.length > 0 ? (
        <div className="pg-grid">
          {photos.map(photo => (
            <div key={photo.id} className="pg-item">
              <div className="pg-img-placeholder">
                <span className="pg-img-icon" aria-hidden>
                  🖼
                </span>
              </div>
              {photo.caption && <p className="pg-caption">{photo.caption}</p>}
              {!readonly && (
                <button
                  type="button"
                  className="pg-delete-btn"
                  onClick={() => handleDelete(photo.id)}
                  disabled={deleting}
                  aria-label="Delete photo"
                >
                  ×
                </button>
              )}
            </div>
          ))}
        </div>
      ) : (
        <p className="pg-empty">No photos uploaded yet.</p>
      )}
    </div>
  );
};

export default PhotoGallery;
