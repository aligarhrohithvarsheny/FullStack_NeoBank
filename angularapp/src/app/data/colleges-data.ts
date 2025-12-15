export interface College {
  name: string;
  type: 'IIT' | 'IIM' | 'University' | 'College';
  state: string;
  city: string;
  address?: string;
  courses?: string[];
}

export const INDIAN_STATES = [
  'Andhra Pradesh', 'Arunachal Pradesh', 'Assam', 'Bihar', 'Chhattisgarh',
  'Goa', 'Gujarat', 'Haryana', 'Himachal Pradesh', 'Jharkhand',
  'Karnataka', 'Kerala', 'Madhya Pradesh', 'Maharashtra', 'Manipur',
  'Meghalaya', 'Mizoram', 'Nagaland', 'Odisha', 'Punjab',
  'Rajasthan', 'Sikkim', 'Tamil Nadu', 'Telangana', 'Tripura',
  'Uttar Pradesh', 'Uttarakhand', 'West Bengal',
  'Andaman and Nicobar Islands', 'Chandigarh', 'Dadra and Nagar Haveli',
  'Daman and Diu', 'Delhi', 'Jammu and Kashmir', 'Ladakh', 'Lakshadweep', 'Puducherry'
];

export const MAJOR_CITIES: { [state: string]: string[] } = {
  'Maharashtra': ['Mumbai', 'Pune', 'Nagpur', 'Nashik', 'Aurangabad'],
  'Karnataka': ['Bangalore', 'Mysore', 'Hubli', 'Mangalore', 'Belgaum'],
  'Tamil Nadu': ['Chennai', 'Coimbatore', 'Madurai', 'Tiruchirappalli', 'Salem'],
  'Delhi': ['New Delhi', 'Delhi'],
  'West Bengal': ['Kolkata', 'Howrah', 'Durgapur', 'Asansol'],
  'Gujarat': ['Ahmedabad', 'Surat', 'Vadodara', 'Rajkot', 'Gandhinagar'],
  'Rajasthan': ['Jaipur', 'Jodhpur', 'Kota', 'Udaipur', 'Ajmer'],
  'Uttar Pradesh': ['Lucknow', 'Kanpur', 'Agra', 'Varanasi', 'Allahabad'],
  'Telangana': ['Hyderabad', 'Warangal', 'Nizamabad'],
  'Andhra Pradesh': ['Visakhapatnam', 'Vijayawada', 'Guntur', 'Tirupati'],
  'Kerala': ['Kochi', 'Thiruvananthapuram', 'Kozhikode', 'Thrissur'],
  'Punjab': ['Chandigarh', 'Ludhiana', 'Amritsar', 'Jalandhar'],
  'Haryana': ['Gurgaon', 'Faridabad', 'Panipat', 'Ambala'],
  'Madhya Pradesh': ['Bhopal', 'Indore', 'Gwalior', 'Jabalpur'],
  'Odisha': ['Bhubaneswar', 'Cuttack', 'Rourkela', 'Berhampur'],
  'Bihar': ['Patna', 'Gaya', 'Bhagalpur', 'Muzaffarpur'],
  'Assam': ['Guwahati', 'Silchar', 'Dibrugarh'],
  'Jharkhand': ['Ranchi', 'Jamshedpur', 'Dhanbad'],
  'Chhattisgarh': ['Raipur', 'Bhilai', 'Bilaspur'],
  'Uttarakhand': ['Dehradun', 'Haridwar', 'Roorkee'],
  'Himachal Pradesh': ['Shimla', 'Dharamshala', 'Mandi'],
  'Goa': ['Panaji', 'Margao', 'Vasco da Gama']
};

export const IIT_COLLEGES: College[] = [
  { name: 'IIT Bombay', type: 'IIT', state: 'Maharashtra', city: 'Mumbai', courses: ['Engineering', 'Technology'] },
  { name: 'IIT Delhi', type: 'IIT', state: 'Delhi', city: 'New Delhi', courses: ['Engineering', 'Technology'] },
  { name: 'IIT Madras', type: 'IIT', state: 'Tamil Nadu', city: 'Chennai', courses: ['Engineering', 'Technology'] },
  { name: 'IIT Kanpur', type: 'IIT', state: 'Uttar Pradesh', city: 'Kanpur', courses: ['Engineering', 'Technology'] },
  { name: 'IIT Kharagpur', type: 'IIT', state: 'West Bengal', city: 'Kharagpur', courses: ['Engineering', 'Technology'] },
  { name: 'IIT Roorkee', type: 'IIT', state: 'Uttarakhand', city: 'Roorkee', courses: ['Engineering', 'Technology'] },
  { name: 'IIT Guwahati', type: 'IIT', state: 'Assam', city: 'Guwahati', courses: ['Engineering', 'Technology'] },
  { name: 'IIT Hyderabad', type: 'IIT', state: 'Telangana', city: 'Hyderabad', courses: ['Engineering', 'Technology'] },
  { name: 'IIT Bangalore', type: 'IIT', state: 'Karnataka', city: 'Bangalore', courses: ['Engineering', 'Technology'] },
  { name: 'IIT Gandhinagar', type: 'IIT', state: 'Gujarat', city: 'Gandhinagar', courses: ['Engineering', 'Technology'] },
  { name: 'IIT Patna', type: 'IIT', state: 'Bihar', city: 'Patna', courses: ['Engineering', 'Technology'] },
  { name: 'IIT Bhubaneswar', type: 'IIT', state: 'Odisha', city: 'Bhubaneswar', courses: ['Engineering', 'Technology'] },
  { name: 'IIT Ropar', type: 'IIT', state: 'Punjab', city: 'Rupnagar', courses: ['Engineering', 'Technology'] },
  { name: 'IIT Indore', type: 'IIT', state: 'Madhya Pradesh', city: 'Indore', courses: ['Engineering', 'Technology'] },
  { name: 'IIT Mandi', type: 'IIT', state: 'Himachal Pradesh', city: 'Mandi', courses: ['Engineering', 'Technology'] },
  { name: 'IIT Varanasi', type: 'IIT', state: 'Uttar Pradesh', city: 'Varanasi', courses: ['Engineering', 'Technology'] },
  { name: 'IIT Palakkad', type: 'IIT', state: 'Kerala', city: 'Palakkad', courses: ['Engineering', 'Technology'] },
  { name: 'IIT Tirupati', type: 'IIT', state: 'Andhra Pradesh', city: 'Tirupati', courses: ['Engineering', 'Technology'] },
  { name: 'IIT Dhanbad', type: 'IIT', state: 'Jharkhand', city: 'Dhanbad', courses: ['Engineering', 'Technology'] },
  { name: 'IIT Bhilai', type: 'IIT', state: 'Chhattisgarh', city: 'Bhilai', courses: ['Engineering', 'Technology'] },
  { name: 'IIT Dharwad', type: 'IIT', state: 'Karnataka', city: 'Dharwad', courses: ['Engineering', 'Technology'] },
  { name: 'IIT Jammu', type: 'IIT', state: 'Jammu and Kashmir', city: 'Jammu', courses: ['Engineering', 'Technology'] },
  { name: 'IIT Goa', type: 'IIT', state: 'Goa', city: 'Ponda', courses: ['Engineering', 'Technology'] }
];

export const IIM_COLLEGES: College[] = [
  { name: 'IIM Ahmedabad', type: 'IIM', state: 'Gujarat', city: 'Ahmedabad', courses: ['MBA', 'PGP', 'FPM'] },
  { name: 'IIM Bangalore', type: 'IIM', state: 'Karnataka', city: 'Bangalore', courses: ['MBA', 'PGP', 'FPM'] },
  { name: 'IIM Calcutta', type: 'IIM', state: 'West Bengal', city: 'Kolkata', courses: ['MBA', 'PGP', 'FPM'] },
  { name: 'IIM Lucknow', type: 'IIM', state: 'Uttar Pradesh', city: 'Lucknow', courses: ['MBA', 'PGP', 'FPM'] },
  { name: 'IIM Indore', type: 'IIM', state: 'Madhya Pradesh', city: 'Indore', courses: ['MBA', 'PGP', 'FPM'] },
  { name: 'IIM Kozhikode', type: 'IIM', state: 'Kerala', city: 'Kozhikode', courses: ['MBA', 'PGP', 'FPM'] },
  { name: 'IIM Shillong', type: 'IIM', state: 'Meghalaya', city: 'Shillong', courses: ['MBA', 'PGP', 'FPM'] },
  { name: 'IIM Raipur', type: 'IIM', state: 'Chhattisgarh', city: 'Raipur', courses: ['MBA', 'PGP', 'FPM'] },
  { name: 'IIM Ranchi', type: 'IIM', state: 'Jharkhand', city: 'Ranchi', courses: ['MBA', 'PGP', 'FPM'] },
  { name: 'IIM Rohtak', type: 'IIM', state: 'Haryana', city: 'Rohtak', courses: ['MBA', 'PGP', 'FPM'] },
  { name: 'IIM Trichy', type: 'IIM', state: 'Tamil Nadu', city: 'Tiruchirappalli', courses: ['MBA', 'PGP', 'FPM'] },
  { name: 'IIM Udaipur', type: 'IIM', state: 'Rajasthan', city: 'Udaipur', courses: ['MBA', 'PGP', 'FPM'] },
  { name: 'IIM Kashipur', type: 'IIM', state: 'Uttarakhand', city: 'Kashipur', courses: ['MBA', 'PGP', 'FPM'] },
  { name: 'IIM Amritsar', type: 'IIM', state: 'Punjab', city: 'Amritsar', courses: ['MBA', 'PGP', 'FPM'] },
  { name: 'IIM Bodh Gaya', type: 'IIM', state: 'Bihar', city: 'Bodh Gaya', courses: ['MBA', 'PGP', 'FPM'] },
  { name: 'IIM Nagpur', type: 'IIM', state: 'Maharashtra', city: 'Nagpur', courses: ['MBA', 'PGP', 'FPM'] },
  { name: 'IIM Visakhapatnam', type: 'IIM', state: 'Andhra Pradesh', city: 'Visakhapatnam', courses: ['MBA', 'PGP', 'FPM'] },
  { name: 'IIM Sambalpur', type: 'IIM', state: 'Odisha', city: 'Sambalpur', courses: ['MBA', 'PGP', 'FPM'] },
  { name: 'IIM Sirmaur', type: 'IIM', state: 'Himachal Pradesh', city: 'Sirmaur', courses: ['MBA', 'PGP', 'FPM'] },
  { name: 'IIM Amritsar', type: 'IIM', state: 'Punjab', city: 'Amritsar', courses: ['MBA', 'PGP', 'FPM'] }
];

export const MAJOR_UNIVERSITIES: College[] = [
  { name: 'University of Delhi', type: 'University', state: 'Delhi', city: 'New Delhi', courses: ['Arts', 'Science', 'Commerce', 'Engineering', 'Law', 'Medicine'] },
  { name: 'Jawaharlal Nehru University', type: 'University', state: 'Delhi', city: 'New Delhi', courses: ['Arts', 'Science', 'Social Sciences', 'Languages'] },
  { name: 'University of Mumbai', type: 'University', state: 'Maharashtra', city: 'Mumbai', courses: ['Arts', 'Science', 'Commerce', 'Engineering', 'Law', 'Medicine'] },
  { name: 'University of Calcutta', type: 'University', state: 'West Bengal', city: 'Kolkata', courses: ['Arts', 'Science', 'Commerce', 'Engineering', 'Law', 'Medicine'] },
  { name: 'University of Madras', type: 'University', state: 'Tamil Nadu', city: 'Chennai', courses: ['Arts', 'Science', 'Commerce', 'Engineering', 'Law', 'Medicine'] },
  { name: 'Banaras Hindu University', type: 'University', state: 'Uttar Pradesh', city: 'Varanasi', courses: ['Arts', 'Science', 'Engineering', 'Medicine', 'Law'] },
  { name: 'Aligarh Muslim University', type: 'University', state: 'Uttar Pradesh', city: 'Aligarh', courses: ['Arts', 'Science', 'Engineering', 'Medicine', 'Law'] },
  { name: 'Jadavpur University', type: 'University', state: 'West Bengal', city: 'Kolkata', courses: ['Engineering', 'Arts', 'Science'] },
  { name: 'Pune University', type: 'University', state: 'Maharashtra', city: 'Pune', courses: ['Arts', 'Science', 'Commerce', 'Engineering', 'Law'] },
  { name: 'Bangalore University', type: 'University', state: 'Karnataka', city: 'Bangalore', courses: ['Arts', 'Science', 'Commerce', 'Engineering', 'Law'] },
  { name: 'Osmania University', type: 'University', state: 'Telangana', city: 'Hyderabad', courses: ['Arts', 'Science', 'Commerce', 'Engineering', 'Law', 'Medicine'] },
  { name: 'Anna University', type: 'University', state: 'Tamil Nadu', city: 'Chennai', courses: ['Engineering', 'Technology'] },
  { name: 'Calcutta University', type: 'University', state: 'West Bengal', city: 'Kolkata', courses: ['Arts', 'Science', 'Commerce', 'Engineering', 'Law', 'Medicine'] },
  { name: 'University of Hyderabad', type: 'University', state: 'Telangana', city: 'Hyderabad', courses: ['Arts', 'Science', 'Social Sciences', 'Engineering'] },
  { name: 'Jamia Millia Islamia', type: 'University', state: 'Delhi', city: 'New Delhi', courses: ['Arts', 'Science', 'Engineering', 'Law', 'Medicine'] }
];

export const MAJOR_COLLEGES: College[] = [
  { name: 'St. Stephen\'s College', type: 'College', state: 'Delhi', city: 'New Delhi', courses: ['Arts', 'Science', 'Commerce'] },
  { name: 'Loyola College', type: 'College', state: 'Tamil Nadu', city: 'Chennai', courses: ['Arts', 'Science', 'Commerce'] },
  { name: 'Presidency College', type: 'College', state: 'Tamil Nadu', city: 'Chennai', courses: ['Arts', 'Science', 'Commerce'] },
  { name: 'Hindu College', type: 'College', state: 'Delhi', city: 'New Delhi', courses: ['Arts', 'Science', 'Commerce'] },
  { name: 'Ramjas College', type: 'College', state: 'Delhi', city: 'New Delhi', courses: ['Arts', 'Science', 'Commerce'] },
  { name: 'Fergusson College', type: 'College', state: 'Maharashtra', city: 'Pune', courses: ['Arts', 'Science', 'Commerce'] },
  { name: 'St. Xavier\'s College', type: 'College', state: 'Maharashtra', city: 'Mumbai', courses: ['Arts', 'Science', 'Commerce'] },
  { name: 'Christ University', type: 'College', state: 'Karnataka', city: 'Bangalore', courses: ['Arts', 'Science', 'Commerce', 'Engineering'] },
  { name: 'St. Joseph\'s College', type: 'College', state: 'Tamil Nadu', city: 'Bangalore', courses: ['Arts', 'Science', 'Commerce'] },
  { name: 'Madras Christian College', type: 'College', state: 'Tamil Nadu', city: 'Chennai', courses: ['Arts', 'Science', 'Commerce'] }
];

export function getAllColleges(): College[] {
  return [...IIT_COLLEGES, ...IIM_COLLEGES, ...MAJOR_UNIVERSITIES, ...MAJOR_COLLEGES];
}

export function getCollegesByType(type: 'IIT' | 'IIM' | 'University' | 'College'): College[] {
  switch (type) {
    case 'IIT': return IIT_COLLEGES;
    case 'IIM': return IIM_COLLEGES;
    case 'University': return MAJOR_UNIVERSITIES;
    case 'College': return MAJOR_COLLEGES;
    default: return [];
  }
}

export function getCollegesByState(state: string): College[] {
  return getAllColleges().filter(college => college.state === state);
}

export function getCollegesByCity(state: string, city: string): College[] {
  return getAllColleges().filter(college => college.state === state && college.city === city);
}

export function getCitiesByState(state: string): string[] {
  return MAJOR_CITIES[state] || [];
}





