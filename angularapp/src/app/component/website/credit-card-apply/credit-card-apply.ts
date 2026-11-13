import { CommonModule, isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, Inject, OnDestroy, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { environment } from '../../../../environment/environment';

interface GuestApplicationResult {
  applicationId: string;
  status: string;
  requestDate: string;
}

interface GuestApplicationStatus {
  applicationId: string;
  status: string;
  requestDate: string;
  processedDate?: string | null;
  approvedLimit?: number | null;
  cardIssued: boolean;
}

@Component({
  selector: 'app-credit-card-apply',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './credit-card-apply.html',
  styleUrls: ['./credit-card-apply.css']
})
export class CreditCardApply implements OnDestroy {
  readonly apiUrl = `${environment.apiBaseUrl}/api/credit-card-requests`;
  readonly maxDate = new Date().toISOString().slice(0, 10);
  application = {
    userName: '',
    userEmail: '',
    mobileNumber: '',
    pan: '',
    aadhaarLastFour: '',
    dateOfBirth: '',
    annualIncome: null as number | null,
    employmentType: 'SALARIED',
    employerName: '',
    designation: '',
    address: '',
    consent: false
  };
  tracking = { applicationId: '', pan: '', mobileNumber: '' };
  companySuggestions: string[] = [];
  panDocument: File | null = null;
  aadhaarDocument: File | null = null;
  selfie: File | null = null;
  selfiePreview = '';
  isCameraOpen = false;
  isSubmitting = false;
  isTracking = false;
  errorMessage = '';
  trackingError = '';
  submittedApplication: GuestApplicationResult | null = null;
  trackedApplication: GuestApplicationStatus | null = null;
  private cameraStream: MediaStream | null = null;
  private readonly isBrowser: boolean;

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnDestroy(): void {
    this.stopCamera();
  }

  searchCompanies(): void {
    const query = this.application.employerName.trim();
    if (query.length < 2) {
      this.companySuggestions = [];
      return;
    }
    const params = new HttpParams().set('query', query);
    this.http.get<string[]>(`${this.apiUrl}/companies`, { params }).subscribe({
      next: names => this.companySuggestions = Array.isArray(names) ? names : [],
      error: error => {
        console.error('Could not load company suggestions:', error);
        this.companySuggestions = [];
      }
    });
  }

  onDocumentSelected(kind: 'pan' | 'aadhaar', event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] || null;
    if (!file) return;
    if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 5 * 1024 * 1024) {
      this.errorMessage = 'Choose a JPEG or PNG image no larger than 5 MB.';
      input.value = '';
      return;
    }
    this.errorMessage = '';
    if (kind === 'pan') this.panDocument = file;
    else this.aadhaarDocument = file;
  }

  async openCamera(): Promise<void> {
    this.errorMessage = '';
    if (!this.isBrowser || !navigator.mediaDevices?.getUserMedia) {
      this.errorMessage = 'Live camera capture is unavailable in this browser. Use HTTPS and allow camera access.';
      return;
    }
    try {
      this.cameraStream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'user' },
        audio: false
      });
      this.isCameraOpen = true;
      setTimeout(() => {
        const video = document.querySelector<HTMLVideoElement>('#credit-card-selfie-video');
        if (video) {
          video.srcObject = this.cameraStream;
          void video.play();
        }
      });
    } catch (error) {
      console.error('Could not start live selfie capture:', error);
      this.errorMessage = 'Camera access was denied or unavailable. Allow camera access and try again.';
    }
  }

  captureSelfie(video: HTMLVideoElement, canvas: HTMLCanvasElement): void {
    if (!video.videoWidth || !video.videoHeight) {
      this.errorMessage = 'Wait for the camera preview to load, then capture your photo.';
      return;
    }
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const context = canvas.getContext('2d');
    if (!context) {
      this.errorMessage = 'The camera image could not be captured.';
      return;
    }
    context.drawImage(video, 0, 0, canvas.width, canvas.height);
    this.selfiePreview = canvas.toDataURL('image/jpeg', 0.9);
    canvas.toBlob(blob => {
      if (!blob) {
        this.errorMessage = 'The camera image could not be captured.';
        return;
      }
      this.selfie = new File([blob], 'live-selfie.jpg', { type: 'image/jpeg' });
      this.errorMessage = '';
      this.stopCamera();
    }, 'image/jpeg', 0.9);
  }

  async submitApplication(): Promise<void> {
    this.errorMessage = '';
    this.submittedApplication = null;
    if (!this.panDocument || !this.aadhaarDocument || !this.selfie) {
      this.errorMessage = 'Upload the PAN and Aadhaar images and capture a live selfie.';
      return;
    }
    if (!this.application.consent) {
      this.errorMessage = 'Please provide consent before submitting your application.';
      return;
    }

    const body = new FormData();
    Object.entries(this.application).forEach(([key, value]) => {
      body.append(key, value === null ? '' : String(value));
    });
    body.set('pan', this.application.pan.trim().toUpperCase());
    body.append('panDocument', this.panDocument);
    body.append('aadhaarDocument', this.aadhaarDocument);
    body.append('selfie', this.selfie);
    this.isSubmitting = true;
    this.http.post<GuestApplicationResult>(`${this.apiUrl}/guest`, body).subscribe({
      next: result => {
        this.submittedApplication = result;
        this.isSubmitting = false;
        this.tracking.applicationId = result.applicationId;
        this.tracking.pan = this.application.pan.trim().toUpperCase();
        this.tracking.mobileNumber = this.application.mobileNumber;
      },
      error: error => {
        this.isSubmitting = false;
        this.errorMessage = error.error?.message || 'Your application could not be submitted. Please try again.';
      }
    });
  }

  trackApplication(): void {
    this.trackingError = '';
    this.trackedApplication = null;
    this.isTracking = true;
    this.http.post<GuestApplicationStatus>(`${this.apiUrl}/track`, {
      ...this.tracking,
      applicationId: this.tracking.applicationId.trim().toUpperCase(),
      pan: this.tracking.pan.trim().toUpperCase(),
      mobileNumber: this.tracking.mobileNumber.trim()
    }).subscribe({
      next: result => {
        this.trackedApplication = result;
        this.isTracking = false;
      },
      error: error => {
        this.trackingError = error.error?.message || 'The application could not be found. Check your details.';
        this.isTracking = false;
      }
    });
  }

  private stopCamera(): void {
    this.cameraStream?.getTracks().forEach(track => track.stop());
    this.cameraStream = null;
    this.isCameraOpen = false;
  }
}
