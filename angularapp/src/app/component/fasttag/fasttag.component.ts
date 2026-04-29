import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-fasttag',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './fasttag.component.html',
  styleUrls: ['./fasttag.component.css']
})
export class FasttagComponent {
  @Input() bankName: string = 'Bank Name';
  @Input() logoUrl: string = ''; // path to bank logo image
  @Input() customerCare: string = '1800 000 0000';
  @Input() barcode: string = '123456-000-7654321';
}
