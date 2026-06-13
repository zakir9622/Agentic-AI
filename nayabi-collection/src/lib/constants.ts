export const INDIAN_STATES = [
  "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh", "Goa",
  "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka", "Kerala",
  "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland",
  "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura",
  "Uttar Pradesh", "Uttarakhand", "West Bengal",
  // Union territories
  "Andaman and Nicobar Islands", "Chandigarh", "Dadra and Nagar Haveli and Daman and Diu",
  "Delhi", "Jammu and Kashmir", "Ladakh", "Lakshadweep", "Puducherry",
] as const;

/** All money values are paise */
export const FREE_SHIPPING_THRESHOLD = Number(process.env.FREE_SHIPPING_THRESHOLD ?? 99900);
export const FLAT_SHIPPING_RATE = 7900; // ₹79
export const COD_CHARGE = Number(process.env.COD_EXTRA_CHARGE ?? 5000); // ₹50
export const GIFT_WRAP_CHARGE = 5000; // ₹50
export const GST_RATE = 5; // % — included in listed prices (textiles)
export const RETURN_WINDOW_DAYS = Number(process.env.RETURN_WINDOW_DAYS ?? 7);
