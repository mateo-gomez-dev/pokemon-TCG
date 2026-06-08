import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  AttachEnergyRequest,
  AttackRequest,
  CreateGameRequest,
  EvolvePokemonRequest,
  GameActionRequest,
  GameResponse,
  JoinGameRequest,
  PlayBasicPokemonRequest,
  PromoteActiveRequest
} from '../models/game.model';

@Injectable({ providedIn: 'root' })
export class GamesService {
  private readonly apiUrl = '/api/games';

  constructor(private readonly http: HttpClient) {
  }

  createGame(request: CreateGameRequest): Observable<GameResponse> {
    return this.http.post<GameResponse>(this.apiUrl, request);
  }

  joinGame(id: number, request: JoinGameRequest): Observable<GameResponse> {
    return this.http.post<GameResponse>(`${this.apiUrl}/${id}/join`, request);
  }

  startGame(id: number): Observable<GameResponse> {
    return this.http.post<GameResponse>(`${this.apiUrl}/${id}/start`, {});
  }

  getGames(): Observable<GameResponse[]> {
    return this.http.get<GameResponse[]>(this.apiUrl);
  }

  getGame(id: number): Observable<GameResponse> {
    return this.http.get<GameResponse>(`${this.apiUrl}/${id}`);
  }

  drawCard(id: number, request: GameActionRequest): Observable<GameResponse> {
    return this.http.post<GameResponse>(`${this.apiUrl}/${id}/actions/draw`, request);
  }

  endTurn(id: number, request: GameActionRequest): Observable<GameResponse> {
    return this.http.post<GameResponse>(`${this.apiUrl}/${id}/actions/end-turn`, request);
  }

  playBasicPokemon(id: number, request: PlayBasicPokemonRequest): Observable<GameResponse> {
    return this.http.post<GameResponse>(`${this.apiUrl}/${id}/actions/play-basic-pokemon`, request);
  }

  attachEnergy(id: number, request: AttachEnergyRequest): Observable<GameResponse> {
    return this.http.post<GameResponse>(`${this.apiUrl}/${id}/actions/attach-energy`, request);
  }

  promoteActive(id: number, request: PromoteActiveRequest): Observable<GameResponse> {
    return this.http.post<GameResponse>(`${this.apiUrl}/${id}/actions/promote-active`, request);
  }

  evolvePokemon(id: number, request: EvolvePokemonRequest): Observable<GameResponse> {
    return this.http.post<GameResponse>(`${this.apiUrl}/${id}/actions/evolve-pokemon`, request);
  }

  attack(id: number, request: AttackRequest): Observable<GameResponse> {
    return this.http.post<GameResponse>(`${this.apiUrl}/${id}/actions/attack`, request);
  }
}
