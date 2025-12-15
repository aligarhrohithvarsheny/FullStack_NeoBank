import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-graphical-password',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './graphical-password.html',
  styleUrls: ['./graphical-password.css']
})
export class GraphicalPasswordComponent implements OnInit {
  @Input() mode: 'setup' | 'verify' = 'verify';
  @Input() requiredSelections: number = 5;
  @Output() passwordComplete = new EventEmitter<number[]>();
  @Output() selectionChanged = new EventEmitter<number[]>();

  // Grid of images (3x3 = 9 images)
  imageGrid: number[] = [1, 2, 3, 4, 5, 6, 7, 8, 9];
  selectedImages: number[] = [];
  isComplete: boolean = false;
  errorMessage: string = '';

  // Image categories for variety
  imageCategories = [
    'animals', 'nature', 'food', 'vehicles', 'sports', 
    'technology', 'travel', 'music', 'art'
  ];

  ngOnInit() {
    this.reset();
  }

  reset() {
    this.selectedImages = [];
    this.isComplete = false;
    this.errorMessage = '';
  }

  selectImage(imageId: number) {
    if (this.isComplete) {
      return;
    }

    // Prevent selecting the same image twice
    if (this.selectedImages.includes(imageId)) {
      this.errorMessage = 'You cannot select the same image twice';
      setTimeout(() => {
        this.errorMessage = '';
      }, 2000);
      return;
    }

    this.selectedImages.push(imageId);
    this.errorMessage = '';
    this.selectionChanged.emit([...this.selectedImages]);

    // Check if required selections are met
    if (this.selectedImages.length >= this.requiredSelections) {
      this.isComplete = true;
      this.passwordComplete.emit([...this.selectedImages]);
    }
  }

  removeLastSelection() {
    if (this.selectedImages.length > 0) {
      this.selectedImages.pop();
      this.isComplete = false;
      this.selectionChanged.emit([...this.selectedImages]);
      this.errorMessage = '';
    }
  }

  clearAll() {
    this.reset();
    this.selectionChanged.emit([]);
  }

  getImageUrl(imageId: number): string {
    // Use a placeholder image service or local images
    // For now, using a placeholder service that provides diverse images
    const category = this.imageCategories[imageId % this.imageCategories.length];
    return `https://picsum.photos/seed/${category}${imageId}/200/200`;
  }

  getSelectionCount(): number {
    return this.selectedImages.length;
  }

  getRemainingSelections(): number {
    return Math.max(0, this.requiredSelections - this.selectedImages.length);
  }
}



