import React from 'react';

const PasswordStrengthMeter = ({ password }) => {
  const getStrength = (pass) => {
    let score = 0;
    if (!pass) return score;
    if (pass.length >= 8) score++;
    if (/[a-z]/.test(pass) && /[A-Z]/.test(pass)) score++;
    if (/\d/.test(pass)) score++;
    if (/[^a-zA-Z\d]/.test(pass)) score++;
    return score;
  };

  const score = getStrength(password);
  
  const getStyles = () => {
    switch (score) {
      case 0: return { width: '0%', color: 'bg-slate-200', label: '' };
      case 1: return { width: '25%', color: 'bg-red-400', label: 'Weak' };
      case 2: return { width: '50%', color: 'bg-orange-400', label: 'Fair' };
      case 3: return { width: '75%', color: 'bg-yellow-400', label: 'Good' };
      case 4: return { width: '100%', color: 'bg-green-500', label: 'Strong' };
      default: return { width: '0%', color: 'bg-slate-200', label: '' };
    }
  };

  const { width, color, label } = getStyles();

  return (
    <div className="mt-2 px-1">
      <div className="flex justify-between items-center mb-1">
        <div className="h-1 w-full bg-slate-100 rounded-full overflow-hidden mr-2">
          <div 
            className={`h-full ${color} transition-all duration-300`} 
            style={{ width }}
          ></div>
        </div>
        <span className="text-[10px] font-bold text-slate-400 whitespace-nowrap min-w-[40px] text-right">
          {label}
        </span>
      </div>
    </div>
  );
};

export default PasswordStrengthMeter;
