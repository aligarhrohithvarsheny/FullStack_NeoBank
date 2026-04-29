export interface EducationLoanApplication {
  id?: number;
  
  // Parent/Applicant Information
  applicantAccountNumber: string;
  applicantName: string;
  applicantEmail: string;
  applicantPan: string;
  applicantAadhar: string;
  applicantMobile: string;
  
  // Child/Student Information
  childName: string;
  childDateOfBirth: string; // ISO date string
  childPlaceOfBirth: string;
  childAge?: number;
  childAccountNumber?: string; // Child's neobank account number (if child has account)
  
  // Education Details - 10th
  tenthSchoolName: string;
  tenthBoard: string;
  tenthPassingYear: number;
  tenthPercentage: number;
  tenthCertificatePath?: string;
  
  // Education Details - 12th
  twelfthSchoolName: string;
  twelfthBoard: string;
  twelfthPassingYear: number;
  twelfthPercentage: number;
  twelfthCertificatePath?: string;
  
  // Education Details - UG
  ugCollegeName: string;
  ugUniversity: string;
  ugCourse: string;
  ugAdmissionYear: number;
  ugExpectedGraduationYear: number;
  ugCurrentCGPA: number;
  ugCertificatePath?: string;
  
  // College/University Details
  collegeType: string; // IIT, IIM, University, College
  collegeName: string;
  collegeUniversity: string;
  collegeState: string;
  collegeCity: string;
  collegeAddress: string;
  collegeCourse: string;
  collegeDegree: string;
  collegeAdmissionYear: number;
  collegeCourseDuration: number;
  collegeFeeAmount: number;
  
  // Documents
  collegeApplicationPath?: string;
  collegeAdmissionLetterPath?: string;
  collegeFeeStructurePath?: string;
  
  // College Account Details
  collegeAccountNumber: string;
  collegeAccountHolderName: string;
  collegeBankName: string;
  collegeBankBranch: string;
  collegeIFSCCode: string;
  
  // Loan Details
  loanId?: number;
  loanAccountNumber?: string;
  requestedLoanAmount: number;
  applicationStatus: 'Pending' | 'Under Review' | 'Approved' | 'Rejected';
  applicationDate?: string;
  lastUpdatedDate?: string;
  
  // Admin Review
  reviewedBy?: string;
  reviewedDate?: string;
  adminNotes?: string;
  rejectionReason?: string;
}

export interface CreateEducationLoanApplication {
  applicantAccountNumber: string;
  applicantName: string;
  applicantEmail: string;
  applicantPan: string;
  applicantAadhar: string;
  applicantMobile: string;
  childName: string;
  childDateOfBirth: string;
  childPlaceOfBirth: string;
  // ... other fields
}

export interface College {
  name: string;
  type: 'IIT' | 'IIM' | 'University' | 'College';
  state: string;
  city: string;
  address?: string;
  courses?: string[];
}


