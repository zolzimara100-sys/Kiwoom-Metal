class StockDto:
    def __init__(self, code, name, market, sector='', industry=''):
        self.code = code
        self.name = name
        self.market = market
        self.sector = sector
        self.industry = industry
    
    def __str__(self):
        return f"StockDto(code={self.code}, name={self.name}, market={self.market}, sector={self.sector}, industry={self.industry})"
    
    def __repr__(self):
        return self.__str__()
