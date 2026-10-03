/**
 * Voice Transcription Service Abstraction
 * Supports progressive enhancement via Web Speech API (SpeechRecognition / webkitSpeechRecognition)
 * with an extensible interface for future server-side audio transcription providers (e.g., Whisper).
 */

export type VoiceState = 'idle' | 'listening' | 'processing' | 'error' | 'unsupported';

export interface VoiceError {
  code: 'permission-denied' | 'no-speech' | 'device-unavailable' | 'network' | 'aborted' | 'unknown';
  message: string;
}

export interface VoiceTranscriptionOptions {
  onStateChange: (state: VoiceState) => void;
  onTranscriptChange: (transcript: string, isFinal: boolean) => void;
  onError: (error: VoiceError) => void;
  lang?: string;
}

export interface IVoiceTranscriptionService {
  isSupported(): boolean;
  start(options: VoiceTranscriptionOptions): Promise<void>;
  stop(): void;
  cancel(): void;
}

declare global {
  interface Window {
    SpeechRecognition?: any;
    webkitSpeechRecognition?: any;
  }
}

export class WebSpeechTranscriptionService implements IVoiceTranscriptionService {
  private recognition: any | null = null;
  private isListening = false;
  private options: VoiceTranscriptionOptions | null = null;

  isSupported(): boolean {
    if (typeof window === 'undefined') return false;
    return !!(window.SpeechRecognition || window.webkitSpeechRecognition);
  }

  async checkPermission(): Promise<'granted' | 'denied' | 'prompt' | 'unknown'> {
    if (typeof navigator === 'undefined' || !navigator.permissions) {
      return 'unknown';
    }
    try {
      // @ts-ignore - name: 'microphone' is standard in Chromium/Firefox
      const status = await navigator.permissions.query({ name: 'microphone' });
      return status.state;
    } catch {
      return 'unknown';
    }
  }

  async start(options: VoiceTranscriptionOptions): Promise<void> {
    if (!this.isSupported()) {
      options.onStateChange('unsupported');
      options.onError({
        code: 'device-unavailable',
        message: 'Speech recognition is not supported in this browser. Please use Chrome, Edge, or Safari, or type your prompt manually.',
      });
      return;
    }

    if (this.isListening) {
      this.stop();
    }

    this.options = options;
    const SpeechRecognitionClass = window.SpeechRecognition || window.webkitSpeechRecognition;

    try {
      this.recognition = new SpeechRecognitionClass();
      this.recognition.continuous = true;
      this.recognition.interimResults = true;
      this.recognition.lang = options.lang || 'en-IN'; // Default to Indian English / English

      this.recognition.onstart = () => {
        this.isListening = true;
        this.options?.onStateChange('listening');
      };

      this.recognition.onresult = (event: any) => {
        let interimTranscript = '';
        let finalTranscript = '';

        for (let i = event.resultIndex; i < event.results.length; ++i) {
          const result = event.results[i];
          const text = result[0]?.transcript || '';
          if (result.isFinal) {
            finalTranscript += text;
          } else {
            interimTranscript += text;
          }
        }

        const currentText = finalTranscript || interimTranscript;
        if (currentText.trim()) {
          this.options?.onTranscriptChange(currentText.trim(), !!finalTranscript);
        }
      };

      this.recognition.onerror = (event: any) => {
        let code: VoiceError['code'] = 'unknown';
        let message = 'An unexpected audio error occurred.';

        switch (event.error) {
          case 'not-allowed':
          case 'service-not-allowed':
            code = 'permission-denied';
            message = 'Microphone permission was denied. Please allow microphone access in your browser settings to use voice input.';
            break;
          case 'no-speech':
            code = 'no-speech';
            message = 'No speech was detected. Please tap the microphone and speak again.';
            break;
          case 'audio-capture':
            code = 'device-unavailable';
            message = 'No microphone device was detected on your device.';
            break;
          case 'network':
            code = 'network';
            message = 'Speech recognition network service error. Check your connection or type your prompt.';
            break;
          case 'aborted':
            code = 'aborted';
            message = 'Speech recording was cancelled.';
            break;
          default:
            message = `Speech recognition error: ${event.error || 'unknown'}`;
        }

        this.isListening = false;
        this.options?.onStateChange('error');
        this.options?.onError({ code, message });
      };

      this.recognition.onend = () => {
        this.isListening = false;
        this.options?.onStateChange('idle');
      };

      this.recognition.start();
    } catch (err: any) {
      this.isListening = false;
      this.options?.onStateChange('error');
      this.options?.onError({
        code: 'unknown',
        message: err?.message || 'Could not start microphone dictation.',
      });
    }
  }

  stop(): void {
    if (this.recognition && this.isListening) {
      this.options?.onStateChange('processing');
      try {
        this.recognition.stop();
      } catch {
        // Ignore stop on inactive instance
      }
    }
    this.isListening = false;
  }

  cancel(): void {
    if (this.recognition) {
      try {
        this.recognition.abort();
      } catch {
        // Ignore abort
      }
    }
    this.isListening = false;
    this.options?.onStateChange('idle');
  }
}

// Default export singleton
export const voiceService = new WebSpeechTranscriptionService();
