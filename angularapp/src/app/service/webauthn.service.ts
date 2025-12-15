import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, from } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root'
})
export class WebAuthnService {

  constructor(private http: HttpClient) {}

  /**
   * Check if WebAuthn is supported in the browser
   */
  isSupported(): boolean {
    return typeof window !== 'undefined' && 
           'PublicKeyCredential' in window && 
           typeof (window as any).PublicKeyCredential !== 'undefined';
  }

  /**
   * Register a new fingerprint credential
   */
  registerCredential(email: string, name: string): Observable<any> {
    // Step 1: Get registration challenge from server
    return this.http.post(`${environment.apiUrl}/webauthn/register/challenge`, { email }).pipe(
      switchMap((challenge: any) => {
        if (!challenge.success) {
          throw new Error(challenge.message || 'Failed to get registration challenge');
        }

        // Step 2: Convert challenge to ArrayBuffer
        const challengeBuffer = this.base64UrlToArrayBuffer(challenge.challenge);
        const userIdBuffer = this.base64UrlToArrayBuffer(challenge.userId);

        // Step 3: Create credential using WebAuthn API
        const publicKeyCredentialCreationOptions: PublicKeyCredentialCreationOptions = {
          challenge: challengeBuffer,
          rp: {
            id: challenge.rpId,
            name: challenge.rpName,
          },
          user: {
            id: userIdBuffer,
            name: email,
            displayName: challenge.userDisplayName || name,
          },
          pubKeyCredParams: challenge.pubKeyCredParams || [
            { type: 'public-key', alg: -7 }, // ES256
            { type: 'public-key', alg: -257 }, // RS256
          ],
          authenticatorSelection: challenge.authenticatorSelection || {
            authenticatorAttachment: 'platform',
            userVerification: 'required',
            requireResidentKey: false,
          },
          timeout: challenge.timeout || 60000,
          attestation: challenge.attestation || 'direct',
        };

        // Step 4: Call WebAuthn API
        return from(
          navigator.credentials.create({
            publicKey: publicKeyCredentialCreationOptions,
          }) as Promise<PublicKeyCredential>
        ).pipe(
          switchMap((credential: any) => {
            // Step 5: Extract credential data
            const response = credential.response as AuthenticatorAttestationResponse;
            const credentialId = this.arrayBufferToBase64Url(credential.rawId);
            
            // Get public key with null check
            const publicKeyBuffer = response.getPublicKey();
            if (!publicKeyBuffer) {
              throw new Error('Failed to get public key from credential');
            }
            const publicKey = this.arrayBufferToBase64Url(publicKeyBuffer);
            
            const clientDataJSON = this.arrayBufferToBase64Url(response.clientDataJSON);
            const attestationObject = this.arrayBufferToBase64Url(response.attestationObject);

            // Detect device name
            const deviceName = this.detectDeviceName();

            // Step 6: Send credential to server for registration
            const credentialData = {
              id: credentialId,
              publicKey: publicKey,
              algorithm: 'ES256', // Default, can be detected from response
              deviceName: deviceName,
              clientDataJSON: clientDataJSON,
              attestationObject: attestationObject,
            };

            return this.http.post(`${environment.apiUrl}/webauthn/register`, {
              email: email,
              credential: credentialData,
            });
          })
        );
      })
    );
  }

  /**
   * Authenticate using fingerprint
   */
  authenticate(email: string): Observable<any> {
    // Step 1: Get authentication challenge from server
    return this.http.post(`${environment.apiUrl}/webauthn/authenticate/challenge`, { email }).pipe(
      switchMap((challenge: any) => {
        if (!challenge.success) {
          throw new Error(challenge.message || 'Failed to get authentication challenge');
        }

        // Step 2: Convert challenge and credential IDs to ArrayBuffer
        const challengeBuffer = this.base64UrlToArrayBuffer(challenge.challenge);
        const allowCredentials = (challenge.allowCredentials || []).map((cred: any) => ({
          id: this.base64UrlToArrayBuffer(cred.id),
          type: cred.type || 'public-key',
        }));

        // Step 3: Create authentication options
        const publicKeyCredentialRequestOptions: PublicKeyCredentialRequestOptions = {
          challenge: challengeBuffer,
          allowCredentials: allowCredentials,
          rpId: challenge.rpId,
          timeout: challenge.timeout || 60000,
          userVerification: challenge.userVerification || 'required',
        };

        // Step 4: Call WebAuthn API
        return from(
          navigator.credentials.get({
            publicKey: publicKeyCredentialRequestOptions,
          }) as Promise<PublicKeyCredential>
        ).pipe(
          switchMap((credential: any) => {
            // Step 5: Extract authentication data
            const response = credential.response as AuthenticatorAssertionResponse;
            
            // Validate required fields
            if (!response.signature || !response.clientDataJSON || !response.authenticatorData) {
              throw new Error('Invalid authentication response: missing required fields');
            }
            
            const credentialId = this.arrayBufferToBase64Url(credential.rawId);
            const signature = this.arrayBufferToBase64Url(response.signature);
            const clientDataJSON = this.arrayBufferToBase64Url(response.clientDataJSON);
            const authenticatorData = this.arrayBufferToBase64Url(response.authenticatorData);
            const userHandle = response.userHandle 
              ? this.arrayBufferToBase64Url(response.userHandle)
              : null;

            // Extract counter from authenticatorData
            // The counter is stored in bytes 33-36 of authenticatorData (4 bytes, big-endian)
            const authenticatorDataBuffer = response.authenticatorData;
            let counter = 0;
            try {
              if (authenticatorDataBuffer.byteLength >= 37) {
                const counterBytes = new Uint8Array(authenticatorDataBuffer.slice(33, 37));
                // Convert 4 bytes (big-endian) to number
                counter = (counterBytes[0] << 24) | (counterBytes[1] << 16) | (counterBytes[2] << 8) | counterBytes[3];
              } else {
                // If authenticatorData is too short, use 0 as fallback
                // The backend will handle this appropriately
                console.warn('AuthenticatorData too short, using counter 0');
                counter = 0;
              }
            } catch (error) {
              console.error('Error extracting counter from authenticatorData:', error);
              // Fallback to 0 - backend will validate
              counter = 0;
            }

            // Step 6: Send authentication data to server
            const authenticationData = {
              credentialId: credentialId,
              signature: signature,
              clientDataJSON: clientDataJSON,
              authenticatorData: authenticatorData,
              userHandle: userHandle,
              counter: counter,
            };

            return this.http.post(`${environment.apiUrl}/webauthn/authenticate`, {
              email: email,
              credential: authenticationData,
            });
          })
        );
      })
    );
  }

  /**
   * Get registered credentials for an admin
   */
  getCredentials(email: string): Observable<any> {
    return this.http.get(`${environment.apiUrl}/webauthn/credentials/${email}`);
  }

  /**
   * Delete a credential
   */
  deleteCredential(email: string, credentialId: string): Observable<any> {
    return this.http.delete(`${environment.apiUrl}/webauthn/credentials/${email}/${credentialId}`);
  }

  /**
   * Helper: Convert base64url string to ArrayBuffer
   */
  private base64UrlToArrayBuffer(base64url: string): ArrayBuffer {
    // Convert base64url to base64
    let base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
    // Add padding if needed
    while (base64.length % 4) {
      base64 += '=';
    }
    // Convert to binary string
    const binaryString = atob(base64);
    // Convert to ArrayBuffer
    const bytes = new Uint8Array(binaryString.length);
    for (let i = 0; i < binaryString.length; i++) {
      bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes.buffer;
  }

  /**
   * Helper: Convert ArrayBuffer to base64url string
   */
  private arrayBufferToBase64Url(buffer: ArrayBuffer): string {
    const bytes = new Uint8Array(buffer);
    let binary = '';
    for (let i = 0; i < bytes.length; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    const base64 = btoa(binary);
    // Convert base64 to base64url
    return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
  }

  /**
   * Detect device name for better UX
   */
  private detectDeviceName(): string {
    const userAgent = navigator.userAgent.toLowerCase();
    if (userAgent.includes('windows')) {
      return 'Windows Hello';
    } else if (userAgent.includes('mac')) {
      return 'Touch ID';
    } else if (userAgent.includes('android')) {
      return 'Android Biometric';
    } else if (userAgent.includes('iphone') || userAgent.includes('ipad')) {
      return 'Face ID / Touch ID';
    }
    return 'Biometric Device';
  }
}

