import React from 'react';
import AuthCard from '../components/AuthCard';
import ChartAnalysisCard from '../components/ChartAnalysisCard';
import DataLinkCard from '../components/DataLinkCard';
import Header from '../components/Header';
import StockListCard from '../components/StockListCard';

const Home: React.FC = () => {
  return (
    <div className="min-h-screen bg-[#f8fafc] py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-5xl mx-auto space-y-6">

        <Header />

        <main className="space-y-4">
          {/* Section 1: Authentication */}
          <section>
            <AuthCard />
          </section>

          {/* Section 2: Stock List Management */}
          <section>
            <StockListCard />
          </section>

          {/* Section 3: Data Collection Link */}
          <section>
            <DataLinkCard />
          </section>

          {/* Section 4: Chart Analysis (Unified) */}
          <section>
            <ChartAnalysisCard />
          </section>
        </main>

        <footer className="mt-12 text-center text-gray-400 text-xs">
          <p>© {new Date().getFullYear()} Kiwoom Data Analytics Dashboard.</p>
        </footer>
      </div>
    </div>
  );
};

export default Home;
