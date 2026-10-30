from model.flan_t5_model import generate_insight

prompt = """
You are a startup business analyst for VentureAI.

Analyze the following startup.

Startup Name: FarmConnect
Industry: Agriculture Technology
Problem: Urban customers have difficulty getting fresh produce directly from local farmers.
Solution: An online platform connecting local farmers directly with urban customers.
Target Market: Urban households looking for fresh locally sourced produce.
Business Model: Marketplace
Revenue Model: Commission on each transaction
BART Viability Score: 78
BART Classification: Viable
BART Confidence: 78
Market Score: 72
Competition Level: 45
Expected Growth Rate: 25 percent
Financial Score: 68
Scalability Score: 80

Generate a concise business analysis.

Return exactly these sections:

Competitive Advantage:
Market Gap:
Differentiation Strategy:
Business Recommendations:
Marketing Recommendations:
Pricing Recommendations:
Risk Analysis:
Final Verdict:

Use only the information provided above.
Do not invent market statistics, revenue figures, competitor revenue,
customer counts, CAGR, TAM, SAM, or SOM.
"""

print("=== VENTUREAI FLAN-T5 BASELINE ===")
result = generate_insight(prompt)
print(result)
