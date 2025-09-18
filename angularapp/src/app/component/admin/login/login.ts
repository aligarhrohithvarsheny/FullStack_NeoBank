import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-login',
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
  imports:[FormsModule],
  standalone: true
  
})


export class Login {
  email: string = '';
  password: string = '';
  errorMessage: string = '';

  constructor(private router: Router) {}

  onSubmit() {
    if (this.email === 'admin@bank.com' && this.password === 'admin123') {
      this.errorMessage = '';
      alert('Login successful ✅');
      this.router.navigate(['/admin/dashboard']);
    } else {
      this.errorMessage = 'Invalid email or password ❌';
    }
  }
}
