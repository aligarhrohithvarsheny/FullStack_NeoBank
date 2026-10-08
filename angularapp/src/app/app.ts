import { Component, signal, AfterViewInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AlertComponent } from './component/shared/alert/alert.component';
import { CopyButtonService } from './service/copy-button.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, AlertComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements AfterViewInit {
  protected readonly title = signal('angularapp');

  constructor(private copyButtons: CopyButtonService) {}

  ngAfterViewInit(): void {
    this.copyButtons.start();
  }
}
