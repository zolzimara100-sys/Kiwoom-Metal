import React from 'react';
import { TrendingUp } from 'lucide-react';

const Header: React.FC = () => {
  return (
    <header className="mb-8 text-center">
      <div className="flex items-center justify-center gap-3 mb-6">
        <div className="p-2 bg-blue-600 rounded-lg shadow-md">
          <TrendingUp className="w-6 h-6 text-white" />
        </div>
        <h1 className="text-3xl font-extrabold text-gray-900 tracking-tight">
          Stock Trading Data Analysis
        </h1>
      </div>
      <div className="relative max-w-2xl mx-auto inline-block text-center">
        <div className="bg-white/60 backdrop-blur-sm px-6 py-3 rounded-full shadow-sm border border-gray-200">
          <p className="text-gray-600 italic font-medium text-base">
            "Those who want to achieve something always find a way, while those who don’t always find an excuse."
          </p>
        </div>
      </div>
    </header>
  );
};

export default Header;