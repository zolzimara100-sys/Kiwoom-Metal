import React from 'react';
import { Link } from 'react-router-dom';
import { PieChart, ArrowRight } from 'lucide-react';

const DataLinkCard: React.FC = () => {
  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 flex flex-col md:flex-row items-center justify-between gap-4 hover:border-blue-300 transition-colors duration-300">

      {/* Content Section */}
      <div className="flex flex-col md:flex-row md:items-center gap-4 md:gap-8 flex-1">

        {/* Requirement: Title "일별 투자자 순매수 세부" font: bold 14, icon on left */}
        <div className="flex items-center gap-2 flex-shrink-0">
           <div className="p-1.5 bg-blue-100 rounded text-blue-600">
              <PieChart className="w-5 h-5" />
           </div>
           <h2 className="text-[14px] font-bold text-gray-900 whitespace-nowrap">
             일별 투자자 순매수 세부
           </h2>
        </div>

        {/* Requirement: Description Text */}
        <div className="h-6 w-px bg-gray-200 hidden md:block"></div>
        <p className="text-gray-600 text-sm md:text-base leading-snug">
          일별 개인.외국인. 기관세부(투신,은행,연금 등)별 순매수 현황
        </p>
      </div>

      {/* Requirement: Link Button */}
      <div className="w-full md:w-auto flex justify-end pt-2 md:pt-0">
        <Link
          to="/data-collection"
          className="inline-flex items-center gap-2 px-5 py-2.5 bg-white border border-gray-300 text-gray-700 rounded-lg font-medium hover:bg-gray-50 hover:text-blue-600 hover:border-blue-300 transition-all duration-200 shadow-sm whitespace-nowrap"
        >
          수집페이지로 이동
          <ArrowRight className="w-4 h-4" />
        </Link>
      </div>

    </div>
  );
};

export default DataLinkCard;