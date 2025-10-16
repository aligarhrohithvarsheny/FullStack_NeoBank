import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AlertService } from '../../../service/alert.service';

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

  constructor(private router: Router, private alertService: AlertService) {}

  onSubmit() {
    if (this.email === 'admin@bank.com' && this.password === 'admin123') {
      this.errorMessage = '';
      this.alertService.loginSuccess('Admin');
      this.router.navigate(['/admin/dashboard']);
    } else {
      this.errorMessage = 'Invalid email or password ❌';
    }
  }
}
