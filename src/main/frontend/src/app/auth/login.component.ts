import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './auth-form.html',
  styleUrls: ['./auth-form.css'],
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly heading = 'Log in';
  readonly submitLabel = 'Log in';
  readonly altText = "Don't have an account?";
  readonly altLink = '/register';
  readonly altLabel = 'Register';
  readonly passwordHint: string | null = null;

  readonly username = signal('');
  readonly password = signal('');
  readonly error = signal<string | null>(null);
  readonly loading = signal(false);

  submit(): void {
    const username = this.username().trim();
    const password = this.password();
    if (!username || !password) {
      this.error.set('Username and password are required.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.auth.login(username, password).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Login failed.');
        this.loading.set(false);
      },
    });
  }
}
