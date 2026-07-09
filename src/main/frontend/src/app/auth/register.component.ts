import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './auth-form.html',
  styleUrls: ['./auth-form.css'],
})
export class RegisterComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly heading = 'Create account';
  readonly submitLabel = 'Register';
  readonly altText = 'Already have an account?';
  readonly altLink = '/login';
  readonly altLabel = 'Log in';
  readonly passwordHint = 'Use at least 12 characters, including a capital letter and a digit.';

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
    if (password.length < 12) {
      this.error.set('Password must be at least 12 characters long.');
      return;
    }
    if (!/[A-Z]/.test(password)) {
      this.error.set('Password must contain at least one capital letter.');
      return;
    }
    if (!/[0-9]/.test(password)) {
      this.error.set('Password must contain at least one digit.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.auth.register(username, password).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Registration failed.');
        this.loading.set(false);
      },
    });
  }
}
