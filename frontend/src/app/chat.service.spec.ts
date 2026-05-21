import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ChatService } from './chat.service';
import { environment } from '../environments/environment';

describe('ChatService', () => {
  let service: ChatService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ChatService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ChatService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getWelcome', () => {
    it('resolves with the message string from the response', async () => {
      const promise = service.getWelcome('alice', 'executive');
      const req = httpMock.expectOne((r) => r.url.includes('/welcome'));
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('username')).toBe('alice');
      expect(req.request.params.get('role')).toBe('executive');
      req.flush({ message: 'Welcome, Alice!' });
      expect(await promise).toBe('Welcome, Alice!');
    });
  });

  describe('ask', () => {
    it('resolves with answer and sources on success', async () => {
      const promise = service.ask('What is our policy?');
      const req = httpMock.expectOne(environment.apiBaseUrl + '/ask');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ question: 'What is our policy?' });
      req.flush({ answer: 'The policy is...', sources: [{ documentName: 'policy.pdf', documentId: 'doc-1' }] });
      const result = await promise;
      expect(result.answer).toBe('The policy is...');
      expect(result.sources).toHaveLength(1);
      expect(result.sources[0].documentName).toBe('policy.pdf');
    });

    it('defaults sources to empty array when backend returns null', async () => {
      const promise = service.ask('Hello');
      httpMock.expectOne(environment.apiBaseUrl + '/ask').flush({ answer: 'Hi', sources: null });
      const result = await promise;
      expect(result.sources).toEqual([]);
    });

    it('rethrows 400 Prompt Blocked as plain Error with the detail message', async () => {
      const promise = service.ask('jailbreak attempt');
      httpMock.expectOne(environment.apiBaseUrl + '/ask').flush(
        { title: 'Prompt Blocked', detail: 'Prompt injection detected' },
        { status: 400, statusText: 'Bad Request' },
      );
      await expect(promise).rejects.toThrow('Prompt injection detected');
    });

    it('rethrows non-400 errors as-is', async () => {
      const promise = service.ask('question');
      httpMock.expectOne(environment.apiBaseUrl + '/ask').flush('Server error', {
        status: 500,
        statusText: 'Internal Server Error',
      });
      await expect(promise).rejects.toBeDefined();
    });
  });

  describe('getHistory', () => {
    it('resolves with the history array from the backend', async () => {
      const promise = service.getHistory();
      httpMock
        .expectOne(environment.apiBaseUrl + '/history')
        .flush([{ role: 'user', text: 'Hello', sources: [] }]);
      const result = await promise;
      expect(result).toHaveLength(1);
      expect(result[0].role).toBe('user');
    });

    it('returns empty array on any HTTP error', async () => {
      const promise = service.getHistory();
      httpMock
        .expectOne(environment.apiBaseUrl + '/history')
        .flush(null, { status: 500, statusText: 'Server Error' });
      expect(await promise).toEqual([]);
    });
  });

  describe('clearHistory', () => {
    it('sends DELETE to /history', async () => {
      const promise = service.clearHistory();
      const req = httpMock.expectOne(environment.apiBaseUrl + '/history');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
      await promise;
    });
  });
});
