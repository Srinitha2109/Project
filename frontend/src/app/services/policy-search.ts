import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class PolicySearchService {
  searchQuery = signal('');

  setQuery(query: string) {
    this.searchQuery.set(query); //updates the search query
  }

  reset() {
    this.searchQuery.set(''); //clears the search query
  }
}
