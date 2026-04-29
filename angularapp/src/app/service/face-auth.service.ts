import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, from, of } from 'rxjs';
import { switchMap, map, catchError } from 'rxjs/operators';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root'
})
export class FaceAuthService {

  private modelsLoaded = false;
  private modelLoadPromise: Promise<void> | null = null;
  private faceapi: any = null;

  constructor(private http: HttpClient) {}

  /**
   * Check if camera is available
   */
  isCameraSupported(): boolean {
    return typeof navigator !== 'undefined' &&
           typeof navigator.mediaDevices !== 'undefined' &&
           typeof navigator.mediaDevices.getUserMedia === 'function';
  }

  /**
   * Dynamically load face-api.js (browser only)
   */
  private async getFaceApi(): Promise<any> {
    if (this.faceapi) return this.faceapi;
    this.faceapi = await import('@vladmandic/face-api');
    return this.faceapi;
  }

  /**
   * Load face-api.js models (only once)
   */
  async loadModels(): Promise<void> {
    if (this.modelsLoaded) return;
    if (this.modelLoadPromise) return this.modelLoadPromise;

    this.modelLoadPromise = (async () => {
      const faceapi = await this.getFaceApi();
      const modelPath = '/assets/models';
      await Promise.all([
        faceapi.nets.ssdMobilenetv1.loadFromUri(modelPath),
        faceapi.nets.faceLandmark68Net.loadFromUri(modelPath),
        faceapi.nets.faceRecognitionNet.loadFromUri(modelPath)
      ]);
      this.modelsLoaded = true;
    })();

    return this.modelLoadPromise;
  }

  /**
   * Open camera and return MediaStream
   */
  async openCamera(videoElement: HTMLVideoElement): Promise<MediaStream> {
    const stream = await navigator.mediaDevices.getUserMedia({
      video: { width: 640, height: 480, facingMode: 'user' }
    });
    videoElement.srcObject = stream;
    await videoElement.play();
    return stream;
  }

  /**
   * Stop camera stream
   */
  stopCamera(stream: MediaStream | null): void {
    if (stream) {
      stream.getTracks().forEach(track => track.stop());
    }
  }

  /**
   * Detect a face, verify eyes are open, and extract 128-dim descriptor.
   * Returns { descriptor, eyesOpen, faceQuality } or null if no face.
   */
  async detectFaceWithEyeCheck(videoElement: HTMLVideoElement): Promise<{
    descriptor: Float32Array;
    eyesOpen: boolean;
    leftEAR: number;
    rightEAR: number;
    faceSize: number;
    confidence: number;
  } | null> {
    const faceapi = await this.getFaceApi();

    const detection = await faceapi
      .detectSingleFace(videoElement, new faceapi.SsdMobilenetv1Options({ minConfidence: 0.6 }))
      .withFaceLandmarks()
      .withFaceDescriptor();

    if (!detection) return null;

    const landmarks = detection.landmarks;
    const positions = landmarks.positions;

    // Eye Aspect Ratio (EAR) for liveness — eyes must be open
    // Left eye: landmarks 36-41, Right eye: landmarks 42-47
    const leftEAR = this.computeEAR(positions, [36, 37, 38, 39, 40, 41]);
    const rightEAR = this.computeEAR(positions, [42, 43, 44, 45, 46, 47]);
    const eyesOpen = leftEAR > 0.2 && rightEAR > 0.2;

    // Face bounding box size check — face must be large enough in frame
    const box = detection.detection.box;
    const faceSize = box.width * box.height;

    return {
      descriptor: detection.descriptor,
      eyesOpen,
      leftEAR,
      rightEAR,
      faceSize,
      confidence: detection.detection.score
    };
  }

  /**
   * Compute Eye Aspect Ratio (EAR) from 6 eye landmark points.
   * EAR > 0.2 means eye is open. EAR < 0.2 means eye is closed.
   */
  private computeEAR(positions: any[], indices: number[]): number {
    const p1 = positions[indices[0]]; // outer corner
    const p2 = positions[indices[1]]; // upper-outer
    const p3 = positions[indices[2]]; // upper-inner
    const p4 = positions[indices[3]]; // inner corner
    const p5 = positions[indices[4]]; // lower-inner
    const p6 = positions[indices[5]]; // lower-outer

    // Vertical distances
    const v1 = Math.sqrt(Math.pow(p2.x - p6.x, 2) + Math.pow(p2.y - p6.y, 2));
    const v2 = Math.sqrt(Math.pow(p3.x - p5.x, 2) + Math.pow(p3.y - p5.y, 2));
    // Horizontal distance
    const h = Math.sqrt(Math.pow(p1.x - p4.x, 2) + Math.pow(p1.y - p4.y, 2));

    if (h === 0) return 0;
    return (v1 + v2) / (2.0 * h);
  }

  /**
   * Legacy method — detect face without eye check
   */
  async detectFace(videoElement: HTMLVideoElement): Promise<Float32Array | null> {
    const result = await this.detectFaceWithEyeCheck(videoElement);
    if (!result) return null;
    return result.descriptor;
  }

  /**
   * Capture multiple face descriptors with eye-open liveness check.
   * Only captures when eyes are confirmed open. Averages descriptors
   * and validates consistency (all captures must be the same person).
   * Returns { descriptor, captureCount } or null.
   */
  async captureMultipleDescriptors(
    videoElement: HTMLVideoElement,
    count: number = 3,
    delayMs: number = 500,
    onStatus?: (status: string) => void
  ): Promise<{ descriptor: Float32Array; captureCount: number } | null> {
    await this.loadModels();
    const descriptors: Float32Array[] = [];
    const minFaceSize = 8000; // minimum face bounding-box area in pixels
    let eyeClosedCount = 0;
    let noFaceCount = 0;
    let tooSmallCount = 0;

    for (let i = 0; i < count * 3 && descriptors.length < count; i++) {
      await new Promise(resolve => setTimeout(resolve, delayMs));
      const result = await this.detectFaceWithEyeCheck(videoElement);

      if (!result) {
        noFaceCount++;
        if (onStatus) onStatus('No face detected. Position your face in the frame.');
        continue;
      }

      if (result.faceSize < minFaceSize) {
        tooSmallCount++;
        if (onStatus) onStatus('Move closer to the camera.');
        continue;
      }

      if (!result.eyesOpen) {
        eyeClosedCount++;
        if (onStatus) onStatus('Eyes closed detected. Please keep your eyes open.');
        continue;
      }

      if (result.confidence < 0.7) {
        if (onStatus) onStatus('Low detection confidence. Hold steady...');
        continue;
      }

      // Check consistency — new descriptor must match previous ones (same person)
      if (descriptors.length > 0) {
        const dist = this.localEuclideanDistance(descriptors[0], result.descriptor);
        if (dist > 0.5) {
          if (onStatus) onStatus('Inconsistent face detected. Hold still and look at camera.');
          continue;
        }
      }

      descriptors.push(result.descriptor);
      if (onStatus) onStatus(`Face captured (${descriptors.length}/${count}). Hold still...`);
    }

    if (descriptors.length === 0) return null;

    // Average all descriptors for a robust representation
    const avg = new Float32Array(128);
    for (const d of descriptors) {
      for (let i = 0; i < 128; i++) {
        avg[i] += d[i];
      }
    }
    for (let i = 0; i < 128; i++) {
      avg[i] /= descriptors.length;
    }
    return { descriptor: avg, captureCount: descriptors.length };
  }

  /**
   * Compute Euclidean distance between two 128-dim Float32Arrays locally
   */
  private localEuclideanDistance(a: Float32Array, b: Float32Array): number {
    let sum = 0;
    for (let i = 0; i < 128; i++) {
      const diff = a[i] - b[i];
      sum += diff * diff;
    }
    return Math.sqrt(sum);
  }

  /**
   * Register face descriptor on the backend
   */
  registerFace(email: string, descriptor: Float32Array, deviceName?: string): Observable<any> {
    return this.http.post(`${environment.apiBaseUrl}/api/face-auth/register`, {
      email,
      faceDescriptor: Array.from(descriptor),
      deviceName: deviceName || this.detectDeviceName()
    });
  }

  /**
   * Verify face descriptor against stored one
   */
  verifyFace(email: string, descriptor: Float32Array): Observable<any> {
    return this.http.post(`${environment.apiBaseUrl}/api/face-auth/verify`, {
      email,
      faceDescriptor: Array.from(descriptor)
    });
  }

  /**
   * Get face auth status for an admin
   */
  getFaceStatus(email: string): Observable<any> {
    return this.http.get(`${environment.apiBaseUrl}/api/face-auth/status/${email}`);
  }

  /**
   * Remove face credentials
   */
  removeFace(email: string): Observable<any> {
    return this.http.delete(`${environment.apiBaseUrl}/api/face-auth/${email}`);
  }

  /**
   * Detect device name
   */
  private detectDeviceName(): string {
    const ua = navigator.userAgent.toLowerCase();
    if (ua.includes('windows')) return 'Windows Laptop Camera';
    if (ua.includes('mac')) return 'MacBook Camera';
    if (ua.includes('linux')) return 'Linux Camera';
    return 'Laptop Camera';
  }
}
